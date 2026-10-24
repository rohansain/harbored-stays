package com.rohan.harbored.repository;

import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Disabled
class BookingAvailabilityTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Listing activeListing;
    private User persistentGuest;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();

        long uniqueSuffix = System.currentTimeMillis();

        User host = User.builder()
                .name("Kavya Host")
                .email("kavya.host_" + uniqueSuffix + "@harbored.com")
                .passwordHash("encoded_bcrypt")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(host);

        persistentGuest = User.builder()
                .name("Arjun Guest")
                .email("arjun.guest_" + uniqueSuffix + "@harbored.com")
                .passwordHash("encoded_bcrypt")
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(persistentGuest);

        activeListing = Listing.builder()
                .title("Furnished 1BHK near Cyber City")
                .description("High speed Wi-Fi, AC, and kitchenette.")
                .location("DLF Phase 2")
                .city("Gurugram")
                .pricePerMonth(new BigDecimal("21000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .owner(host)
                .build();
        listingRepository.save(activeListing);

        // Seed an existing confirmed reservation from July 1, 2026 to September 1, 2026 (2 months)
        Booking confirmedBooking = Booking.builder()
                .listing(activeListing)
                .guest(persistentGuest)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 9, 1))
                .totalPrice(new BigDecimal("42000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();
        bookingRepository.save(confirmedBooking);
        entityManager.flush();
    }

    @Test
    @DisplayName("Case 1: Exact Match Overlap - Should detect conflict")
    void shouldDetectConflictOnExactMatch() {
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                activeListing.getId(),
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 1)
        );
        assertThat(hasOverlap).isTrue();
    }

    @Test
    @DisplayName("Case 2: Enclosing Overlap (Requested dates encompass existing stay) - Should detect conflict")
    void shouldDetectConflictWhenEnclosingExistingStay() {
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                activeListing.getId(),
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 9, 15)
        );
        assertThat(hasOverlap).isTrue();
    }

    @Test
    @DisplayName("Case 3: Internal Overlap (Requested dates fall completely inside existing stay) - Should detect conflict")
    void shouldDetectConflictWhenInsideExistingStay() {
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                activeListing.getId(),
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 8, 15)
        );
        assertThat(hasOverlap).isTrue();
    }

    @Test
    @DisplayName("Case 4: Partial Border Overlap (Starts before, ends in middle) - Should detect conflict")
    void shouldDetectConflictWhenPartialStartOverlap() {
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                activeListing.getId(),
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 7, 15)
        );
        assertThat(hasOverlap).isTrue();
    }

    @Test
    @DisplayName("Case 5: Partial Border Overlap (Starts in middle, ends after) - Should detect conflict")
    void shouldDetectConflictWhenPartialEndOverlap() {
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                activeListing.getId(),
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 10, 1)
        );
        assertThat(hasOverlap).isTrue();
    }

    @Test
    @DisplayName("Case 6: Adjacent Dates (Requested stay ends exactly when existing stay begins) - Should NOT conflict")
    void shouldAllowAdjacentStayEndingOnStartDate() {
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                activeListing.getId(),
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 7, 1)
        );
        assertThat(hasOverlap).isFalse();
    }

    @Test
    @DisplayName("Case 7: Adjacent Dates (Requested stay begins exactly when existing stay ends) - Should NOT conflict")
    void shouldAllowAdjacentStayBeginningOnEndDate() {
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                activeListing.getId(),
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 1)
        );
        assertThat(hasOverlap).isFalse();
    }

    @Test
    @DisplayName("Case 8: Ignored Status (Overlaps with CANCELLED booking) - Should NOT conflict")
    void shouldIgnoreCancelledBookings() {
        Booking cancelledBooking = Booking.builder()
                .listing(activeListing)
                .guest(persistentGuest)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 12, 1))
                .totalPrice(new BigDecimal("21000.00"))
                .status(BookingStatus.CANCELLED)
                .build();
        bookingRepository.save(cancelledBooking);
        entityManager.flush();

        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                activeListing.getId(),
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 11, 5),
                LocalDate.of(2026, 11, 25)
        );
        assertThat(hasOverlap).isFalse();
    }
}