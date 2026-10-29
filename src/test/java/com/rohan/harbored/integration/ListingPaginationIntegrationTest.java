package com.rohan.harbored.integration;

import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.dto.response.PagedResponseDto;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.ListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListingPaginationIntegrationTest {

    @Autowired
    private ListingService listingService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        long unique = System.currentTimeMillis();
        User host = User.builder()
                .name("Pagination Host")
                .email("page_host_" + unique + "@harbored.com")
                .passwordHash("hashed_pwd")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(host);

        // Seed 25 listings in Bengaluru
        List<Listing> seedList = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            seedList.add(Listing.builder()
                    .title("Bengaluru Flat #" + i)
                    .description("High-speed Wi-Fi included")
                    .location("Whitefield")
                    .city("Bengaluru")
                    .pricePerMonth(BigDecimal.valueOf(10000 + (i * 500)))
                    .minStayMonths(1)
                    .maxStayMonths(6)
                    .owner(host)
                    .build());
        }
        listingRepository.saveAll(seedList);
    }

    @Test
    @DisplayName("Should return exactly requested page size and correct page metadata")
    void shouldReturnPaginatedResults() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .city("Bengaluru")
                .build();

        Pageable firstPage = PageRequest.of(0, 10, Sort.by("pricePerMonth").ascending());
        PagedResponseDto<ListingResponseDto> response = listingService.searchListings(filter, firstPage);

        assertThat(response.getContent()).hasSize(10);
        assertThat(response.getTotalElements()).isEqualTo(25);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
        assertThat(response.isHasNext()).isTrue();

        // Check ascending sort: first element price < last element price
        BigDecimal firstPrice = response.getContent().get(0).getPricePerMonth();
        BigDecimal lastPrice = response.getContent().get(9).getPricePerMonth();
        assertThat(firstPrice).isLessThan(lastPrice);
    }

    @Test
    @DisplayName("Should slice subsequent page correctly")
    void shouldFetchSecondPage() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .city("Bengaluru")
                .build();

        Pageable secondPage = PageRequest.of(1, 10, Sort.by("pricePerMonth").ascending());
        PagedResponseDto<ListingResponseDto> response = listingService.searchListings(filter, secondPage);

        assertThat(response.getContent()).hasSize(10);
        assertThat(response.getPageNumber()).isEqualTo(1);
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    @DisplayName("Should fetch last partial page correctly")
    void shouldFetchLastPartialPage() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .city("Bengaluru")
                .build();

        Pageable thirdPage = PageRequest.of(2, 10, Sort.by("pricePerMonth").ascending());
        PagedResponseDto<ListingResponseDto> response = listingService.searchListings(filter, thirdPage);

        assertThat(response.getContent()).hasSize(5); // 25 total: 10 + 10 + 5
        assertThat(response.getPageNumber()).isEqualTo(2);
        assertThat(response.isLast()).isTrue();
        assertThat(response.isHasNext()).isFalse();
    }
}