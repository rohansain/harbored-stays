package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.dto.response.PagedResponseDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    @DisplayName("Should successfully filter listings by city and price range with pagination")
    void shouldFilterListingsSuccessfully() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .city("Gurugram")
                .minPrice(new BigDecimal("15000.00"))
                .maxPrice(new BigDecimal("25000.00"))
                .build();

        Pageable pageable = PageRequest.of(0, 10);

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

        Page<Listing> pagedListing = new PageImpl<>(List.of(sampleListing), pageable, 1);

        when(listingRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagedListing);
        when(listingMapper.toResponseDto(sampleListing)).thenReturn(mappedDto);

        PagedResponseDto<ListingResponseDto> response = listingService.searchListings(filter, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getPageNumber()).isZero();
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();

        ListingResponseDto resultDto = response.getContent().get(0);
        assertThat(resultDto.getCity()).isEqualTo("Gurugram");
        assertThat(resultDto.getPricePerMonth()).isEqualTo(new BigDecimal("20000.00"));

        verify(listingRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(listingMapper).toResponseDto(sampleListing);
    }

    @Test
    @DisplayName("Should reject search when minPrice is greater than maxPrice before hitting database")
    void shouldRejectInvalidPriceRange() {
        ListingSearchFilterDto invalidFilter = ListingSearchFilterDto.builder()
                .minPrice(new BigDecimal("50000.00"))
                .maxPrice(new BigDecimal("20000.00"))
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> listingService.searchListings(invalidFilter, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("minPrice cannot be greater than maxPrice");

        verify(listingRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}