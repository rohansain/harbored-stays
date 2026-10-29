package com.rohan.harbored.integration;

import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    private Listing whitefieldFlat;
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

        // Listing 1: Gurugram, ₹22,000, mentions "Cyber City" in title, has Wi-Fi in description
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

        // Listing 2: Bengaluru, ₹28,000, mentions "IT Park" in description
        whitefieldFlat = Listing.builder()
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

        // Listing 3: Noida, ₹18,000, mentions "Wi-Fi", booked in December 2026
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

        // Seed confirmed booking on Listing 3 (Noida) for December 2026
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

        List<ListingResponseDto> results = listingService.searchListings(filter);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).contains("Cyber City");
    }

    @Test
    @DisplayName("Keyword in description should return matching listing")
    void shouldFindListingByKeywordInDescription() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .keyword("IT Park")
                .build();

        List<ListingResponseDto> results = listingService.searchListings(filter);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Modern 1BHK in Whitefield");
    }

    @Test
    @DisplayName("Combined search: Keyword + City + Price + Date availability")
    void shouldExecuteCombinedSearchSuccessfully() {
        // Search for "Wi-Fi", maxPrice: ₹20000, in December 2026
        // Listing 3 matches "Wi-Fi" and price <= 20000, BUT is booked in December!
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .keyword("Wi-Fi")
                .maxPrice(new BigDecimal("20000.00"))
                .startDate(LocalDate.of(2026, 12, 5))
                .endDate(LocalDate.of(2026, 12, 25))
                .build();

        List<ListingResponseDto> results = listingService.searchListings(filter);

        // Listing 3 was excluded due to the overlapping booking, Listing 1 excluded due to price (₹22,000 > ₹20,000)
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Combined search matches listing when all criteria and dates align")
    void shouldMatchWhenAllCombinedCriteriaAlign() {
        ListingSearchFilterDto filter = ListingSearchFilterDto.builder()
                .keyword("Wi-Fi")
                .city("Gurugram")
                .minPrice(new BigDecimal("20000.00"))
                .maxPrice(new BigDecimal("25000.00"))
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .build();

        List<ListingResponseDto> results = listingService.searchListings(filter);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(cyberHubStudio.getId());
        assertThat(results.get(0).getCity()).isEqualTo("Gurugram");
    }
}