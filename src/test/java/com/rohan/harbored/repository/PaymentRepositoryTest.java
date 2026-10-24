package com.rohan.harbored.repository;

import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.Payment;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.PaymentStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Booking persistentBooking;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();

        long uniqueSuffix = System.currentTimeMillis();

        User host = User.builder()
                .name("Super Landlord")
                .email("host_" + uniqueSuffix + "@harbored.com")
                .passwordHash("secret_bcrypt")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(host);

        User guest = User.builder()
                .name("Tech Intern")
                .email("guest_" + uniqueSuffix + "@harbored.com")
                .passwordHash("secret_bcrypt")
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(guest);

        Listing listing = Listing.builder()
                .title("Cozy Studio near Cyber Hub")
                .description("Includes fast Wi-Fi and power backup.")
                .location("Sector 24")
                .city("Gurugram")
                .pricePerMonth(new BigDecimal("22000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .owner(host)
                .build();
        listingRepository.save(listing);

        persistentBooking = Booking.builder()
                .listing(listing)
                .guest(guest)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(32))
                .totalPrice(new BigDecimal("22000.00"))
                .status(BookingStatus.PENDING)
                .build();
        bookingRepository.save(persistentBooking);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should save a Payment record and fetch by Razorpay Order ID")
    void shouldSaveAndFetchPaymentByOrderId() {
        Payment payment = Payment.builder()
                .booking(persistentBooking)
                .razorpayOrderId("order_TEST_123456789")
                .razorpaySignature("test_signature_123")
                .amount(new BigDecimal("22000.00"))
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        entityManager.flush();

        assertThat(savedPayment.getId()).isNotNull();
        assertThat(savedPayment.getCreatedAt()).isNotNull();

        Optional<Payment> fetchedPayment = paymentRepository.findByRazorpayOrderId("order_TEST_123456789");
        assertThat(fetchedPayment).isPresent();
        assertThat(fetchedPayment.get().getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(fetchedPayment.get().getBooking().getId()).isEqualTo(persistentBooking.getId());
    }
}