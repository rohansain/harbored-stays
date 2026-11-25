package com.rohan.harbored.integration;

import com.rohan.harbored.config.RedisConfig;
import com.rohan.harbored.dto.request.CreateListingDto;
import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.dto.response.ListingAvailabilityResponseDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.dto.response.PagedResponseDto;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.ListingAvailabilityService;
import com.rohan.harbored.service.ListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class RedisCachingIntegrationTest {

    @Autowired
    private ListingService listingService;

    @Autowired
    private ListingAvailabilityService listingAvailabilityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockitoSpyBean
    private ListingRepository listingRepository;

    @MockitoSpyBean
    private BookingRepository bookingRepository;

    private User verifiedHost;
    private Listing savedListing;

    @BeforeEach
    void setUp() {
        clearCache(RedisConfig.SEARCH_RESULTS_CACHE);
        clearCache(RedisConfig.AVAILABILITY_CACHE);

        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        verifiedHost = userRepository.save(User.builder()
                .name("Rohan Host")
                .email("host.cachetest@harbored.com")
                .passwordHash("encodedPassword123")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build());

        savedListing = listingRepository.save(Listing.builder()
                .title("Noida Sector 144 Studio")
                .description("Fully furnished studio near Expressway")
                .location("Sector 144")
                .city("Noida")
                .pricePerMonth(BigDecimal.valueOf(14000))
                .minStayMonths(1)
                .maxStayMonths(12)
                .owner(verifiedHost)
                .build());

        clearInvocations(listingRepository, bookingRepository);
    }

    @Test
    @DisplayName("Search results hit DB once, then serve normalized queries from Redis")
    void searchListings_cachesNormalizedResultsInRedis() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        ListingSearchFilterDto firstFilter = new ListingSearchFilterDto();
        firstFilter.setCity("Noida");
        firstFilter.setMaxPrice(BigDecimal.valueOf(15000));

        ListingSearchFilterDto messySameFilter = new ListingSearchFilterDto();
        messySameFilter.setCity("  NOIDA ");
        messySameFilter.setMaxPrice(new BigDecimal("15000.00"));

        // 1st call: Cache MISS -> hits PostgreSQL
        PagedResponseDto<ListingResponseDto> firstResult = listingService.searchListings(firstFilter, pageable);
        assertThat(firstResult.getContent()).hasSize(1);
        verify(listingRepository, times(1)).findAll(any(Specification.class), eq(pageable));

        // 2nd call with messy casing/decimal scale: Cache HIT -> zero extra DB calls
        PagedResponseDto<ListingResponseDto> secondResult = listingService.searchListings(messySameFilter, pageable);
        assertThat(secondResult.getContent()).hasSize(1);
        verify(listingRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Updating a listing evicts listingSearchResults cache and reloads fresh data")
    void updateListing_evictsSearchCache() {
        Pageable pageable = PageRequest.of(0, 10);
        ListingSearchFilterDto filter = new ListingSearchFilterDto();
        filter.setCity("Noida");

        // Warm up cache
        listingService.searchListings(filter, pageable);
        verify(listingRepository, times(1)).findAll(any(Specification.class), eq(pageable));

        // Mutate listing -> triggers @CacheEvict
        CreateListingDto updateRequest = CreateListingDto.builder()
                .title("Updated Luxury Studio")
                .description("Updated description")
                .location("Sector 144")
                .city("Noida")
                .pricePerMonth(BigDecimal.valueOf(16000))
                .minStayMonths(1)
                .maxStayMonths(12)
                .build();

        listingService.updateListing(savedListing.getId(), verifiedHost.getEmail(), updateRequest);

        // Next search MUST hit DB again and return updated title
        PagedResponseDto<ListingResponseDto> afterEviction = listingService.searchListings(filter, pageable);
        assertThat(afterEviction.getContent().get(0).getTitle()).isEqualTo("Updated Luxury Studio");
        verify(listingRepository, times(2)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Concurrent availability checks coalesce into a single DB query via sync=true")
    void checkAvailability_preventsCacheStampedeUnderConcurrency() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = startDate.plusMonths(2);

        int concurrentUsers = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);

        Callable<ListingAvailabilityResponseDto> task = () ->
                listingAvailabilityService.checkAvailability(savedListing.getId(), startDate, endDate);

        List<Future<ListingAvailabilityResponseDto>> futures = executor.invokeAll(
                java.util.Collections.nCopies(concurrentUsers, task)
        );
        executor.shutdown();

        for (Future<ListingAvailabilityResponseDto> future : futures) {
            assertThat(future.get().isAvailable()).isTrue();
        }

        // Because sync = true locks per key, only 1 thread queried BookingRepository
        verify(bookingRepository, times(1))
                .existsByListingIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
                        eq(savedListing.getId()),
                        any(),
                        eq(endDate),
                        eq(startDate)
                );
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}