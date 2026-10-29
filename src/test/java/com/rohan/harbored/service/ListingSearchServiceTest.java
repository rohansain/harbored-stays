package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.mapper.ListingMapper;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.impl.ListingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingSearchServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingMapper listingMapper;

    @Mock
    private DocumentStorageService documentStorageService;

    @InjectMocks
    private ListingServiceImpl listingService;

    @Test
    @DisplayName("Should successfully filter listings by city and price range")
    void shouldFilterListingsSuccessfully() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .city("Gurugram")
                .minPrice(new BigDecimal("15000.00"))
                .maxPrice(new BigDecimal("25000.00"))
                .build();

        Listing sampleListing = Listing.builder()
                .id(1L)
                .title("Cozy Room in DLF Phase 2")
                .city("Gurugram")
                .pricePerMonth(new BigDecimal("20000.00"))
                .owner(User.builder().id(10L).build())
                .build();

        ListingResponseDto mappedDto = ListingResponseDto.builder()
                .id(1L)
                .title("Cozy Room in DLF Phase 2")
                .city("Gurugram")
                .pricePerMonth(new BigDecimal("20000.00"))
                .build();

        when(listingRepository.findAll(any(Specification.class))).thenReturn(List.of(sampleListing));
        when(listingMapper.toResponseDto(sampleListing)).thenReturn(mappedDto);

        List<ListingResponseDto> results = listingService.searchListings(filter);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCity()).isEqualTo("Gurugram");
        assertThat(results.get(0).getPricePerMonth()).isEqualTo(new BigDecimal("20000.00"));
        verify(listingRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Should reject search when minPrice is greater than maxPrice")
    void shouldRejectInvalidPriceRange() {
        ListingSearchFilterDto invalidFilter = ListingSearchFilterDto.builder()
                .minPrice(new BigDecimal("50000.00"))
                .maxPrice(new BigDecimal("20000.00"))
                .build();

        assertThatThrownBy(() -> listingService.searchListings(invalidFilter))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("minPrice cannot be greater than maxPrice");
    }
}