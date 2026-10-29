
package com.rohan.harbored.integration;

import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.dto.response.PagedResponseDto;
import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListingCombinedSearchTest {

    @Autowired
    private ListingService listingService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    private Listing cyberHubStudio;
    private Listing noidaApartment;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        long unique = System.currentTimeMillis();

        User host = User.builder()
                .name("Super Host")
                .email("host_" + unique + "@harbored.com")
                .passwordHash("hashed_pwd")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        userRepository.save(host);

        User guest = User.builder()
                .name("Tech Guest")
                .email("guest_" + unique + "@harbored.com")
                .passwordHash("hashed_pwd")
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        userRepository.save(guest);

        cyberHubStudio = Listing.builder()
                .title("Furnished Studio near Cyber City")
                .description("Includes high-speed Wi-Fi and power backup.")
                .location("DLF Phase 2")
                .city("Gurugram")
                .pricePerMonth(new BigDecimal("22000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .owner(host)
                .build();

        listingRepository.save(cyberHubStudio);

        Listing whitefieldFlat = Listing.builder()
                .title("Modern 1BHK in Whitefield")
                .description("Walking distance to IT Park, includes modern kitchen.")
                .location("EPIP Zone")
                .city("Bengaluru")
                .pricePerMonth(new BigDecimal("28000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .owner(host)
                .build();

        listingRepository.save(whitefieldFlat);

        noidaApartment = Listing.builder()
                .title("Cozy Flat in Sector 62")
                .description("Dedicated study desk with fast Wi-Fi.")
                .location("Sector 62")
                .city("Noida")
                .pricePerMonth(new BigDecimal("18000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .owner(host)
                .build();

        listingRepository.save(noidaApartment);

        Booking confirmedBooking = Booking.builder()
                .listing(noidaApartment)
                .guest(guest)
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .totalPrice(new BigDecimal("18000.00"))
                .status(BookingStatus.CONFIRMED)
                .hostApproved(true)
                .build();

        bookingRepository.save(confirmedBooking);
    }

    @Test
    @DisplayName("Keyword in title should return matching listing")
    void shouldFindListingByKeywordInTitle() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .keyword("Cyber")
                .build();

        PagedResponseDto<ListingResponseDto> results =
                listingService.searchListings(filter, PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getTitle())
                .contains("Cyber City");
    }

    @Test
    @DisplayName("Keyword in description should return matching listing")
    void shouldFindListingByKeywordInDescription() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .keyword("IT Park")
                .build();

        PagedResponseDto<ListingResponseDto> results =
                listingService.searchListings(filter, PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getTitle())
                .isEqualTo("Modern 1BHK in Whitefield");
    }

    @Test
    @DisplayName("Combined search excludes booked or over-budget listings")
    void shouldExecuteCombinedSearchSuccessfully() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .keyword("Wi-Fi")
                .maxPrice(new BigDecimal("20000.00"))
                .startDate(LocalDate.of(2026, 12, 5))
                .endDate(LocalDate.of(2026, 12, 25))
                .build();

        PagedResponseDto<ListingResponseDto> results =
                listingService.searchListings(filter, PageRequest.of(0, 10));

        assertThat(results.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Combined search returns listing when all criteria match")
    void shouldMatchWhenAllCombinedCriteriaAlign() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .keyword("Wi-Fi")
                .city("Gurugram")
                .minPrice(new BigDecimal("20000.00"))
                .maxPrice(new BigDecimal("25000.00"))
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .build();

        PagedResponseDto<ListingResponseDto> results =
                listingService.searchListings(filter, PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getId())
                .isEqualTo(cyberHubStudio.getId());
        assertThat(results.getContent().get(0).getCity())
                .isEqualTo("Gurugram");
    }
}
