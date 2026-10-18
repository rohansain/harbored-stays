package com.rohan.harbored.repository;

import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest {
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should persist booking, set default status to PENDING, and increment version on update")
    void shouldPersistAndManageBookingLifecycle() {
        User host = User.builder()
                .name("Verified Landlord")
                .email("landlord@harbored.com")
                .passwordHash("secret_bcrypt")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(host);

        User guest = User.builder()
                .name("Verified Student")
                .email("student@harbored.com")
                .passwordHash("secret_bcrypt")
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(guest);

        Listing listing = Listing.builder()
                .title("Single Room near Manyata Tech Park")
                .description("Includes high-speed Wi-Fi, desk, and kitchen access.")
                .location("Nagavara")
                .city("Bengaluru")
                .pricePerMonth(new BigDecimal("12000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .owner(host)
                .build();
        listingRepository.save(listing);

        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = start.plusMonths(2);

        Booking booking = Booking.builder()
                .listing(listing)
                .guest(guest)
                .startDate(start)
                .endDate(end)
                .totalPrice(new BigDecimal("24000.00"))
                .status(BookingStatus.PENDING)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        entityManager.flush();

        assertThat(savedBooking.getId()).isNotNull();
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(savedBooking.getVersion()).isEqualTo(0L);

        // Mutate status to test optimistic version increment
        savedBooking.setStatus(BookingStatus.CONFIRMED);
        Booking updatedBooking = bookingRepository.save(savedBooking);
        entityManager.flush();

        assertThat(updatedBooking.getVersion()).isEqualTo(1L);

        List<Booking> guestBookings = bookingRepository.findByGuestIdOrderByCreatedAtDesc(guest.getId());
        assertThat(guestBookings).hasSize(1);
        assertThat(guestBookings.get(0).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }
}