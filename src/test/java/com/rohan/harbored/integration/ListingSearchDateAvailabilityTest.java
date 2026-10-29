package com.rohan.harbored.integration;

import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.exception.BadRequestException;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListingSearchDateAvailabilityTest {

    @Autowired
    private ListingService listingService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    private Listing availableListing;
    private Listing bookedListing;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        long uniqueSuffix = System.currentTimeMillis();

        User host = User.builder()
                .name("Kavya Host")
                .email("host_" + uniqueSuffix + "@harbored.com")
                .passwordHash("hashed_pwd")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(host);

        User guest = User.builder()
                .name("Arjun Guest")
                .email("guest_" + uniqueSuffix + "@harbored.com")
                .passwordHash("hashed_pwd")
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(guest);

        // Listing 1: Fully open
        availableListing = Listing.builder()
                .title("Sunny 1BHK in Koramangala")
                .description("Furnished studio with balcony.")
                .location("5th Block")
                .city("Bengaluru")
                .pricePerMonth(new BigDecimal("22000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .owner(host)
                .build();
        listingRepository.save(availableListing);

        // Listing 2: Has a CONFIRMED booking from Nov 1 to Dec 1, 2026
        bookedListing = Listing.builder()
                .title("Quiet Room in HSR Layout")
                .description("Near tech offices.")
                .location("Sector 1")
                .city("Bengaluru")
                .pricePerMonth(new BigDecimal("24000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .owner(host)
                .build();
        listingRepository.save(bookedListing);

        Booking confirmedBooking = Booking.builder()
                .listing(bookedListing)
                .guest(guest)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 12, 1))
                .totalPrice(new BigDecimal("24000.00"))
                .status(BookingStatus.CONFIRMED)
                .hostApproved(true)
                .build();
        bookingRepository.save(confirmedBooking);
    }

    @Test
    @DisplayName("Should exclude booked listing when requested search dates overlap with confirmed stay")
    void shouldExcludeOverlappingBookedListing() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .city("Bengaluru")
                .startDate(LocalDate.of(2026, 11, 10))
                .endDate(LocalDate.of(2026, 11, 25))
                .build();

        List<ListingResponseDto> results = listingService.searchListings(filter);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(availableListing.getId());
        assertThat(results.get(0).getTitle()).isEqualTo("Sunny 1BHK in Koramangala");
    }

    @Test
    @DisplayName("Should include booked listing when requested search dates are after the confirmed stay")
    void shouldIncludeListingWhenDatesDoNotOverlap() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .city("Bengaluru")
                .startDate(LocalDate.of(2026, 12, 5))
                .endDate(LocalDate.of(2027, 1, 5))
                .build();

        List<ListingResponseDto> results = listingService.searchListings(filter);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Should reject search when only one date boundary is provided")
    void shouldRejectPartialDateSearch() {
        ListingSearchFilterDto invalidFilter = ListingSearchFilterDto.builder()
                .startDate(LocalDate.of(2026, 11, 1))
                .build();

        assertThatThrownBy(() -> listingService.searchListings(invalidFilter))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Both startDate and endDate must be provided together");
    }
}