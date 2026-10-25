package com.rohan.harbored.integration;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.rohan.harbored.dto.request.CreateBookingDto;
import com.rohan.harbored.dto.response.BookingResponseDto;
import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.Payment;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.PaymentStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.exception.UnauthorizedException;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.PaymentRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.BookingService;
import com.rohan.harbored.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingPaymentIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private RazorpayClient razorpayClient;

    private User host;
    private User guest;
    private Listing listing;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        long timestamp = System.currentTimeMillis();

        host = User.builder()
                .name("Kavya Host")
                .email("kavya_" + timestamp + "@harbored.com")
                .passwordHash("hashed_pwd")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(host);

        guest = User.builder()
                .name("Arjun Guest")
                .email("arjun_" + timestamp + "@harbored.com")
                .passwordHash("hashed_pwd")
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(guest);

        listing = Listing.builder()
                .title("Cozy Studio near Manyata Tech Park")
                .description("Includes high-speed Wi-Fi, kitchen, and power backup.")
                .location("Nagavara")
                .city("Bengaluru")
                .pricePerMonth(new BigDecimal("30000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .owner(host)
                .build();
        listingRepository.save(listing);

        ReflectionTestUtils.setField(paymentService, "razorpayWebhookSecret", "test_webhook_secret_2026");
    }

    @Test
    @DisplayName("End-to-End Success: Book -> Host Accept -> Webhook Paid -> Confirmed")
    void shouldExecuteFullBookingPaymentLifecycle() {
        // 1. Guest creates PENDING booking
        CreateBookingDto bookingRequest = CreateBookingDto.builder()
                .listingId(listing.getId())
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(32))
                .build();

        BookingResponseDto bookingDto = bookingService.createBooking(guest.getEmail(), bookingRequest);
        assertThat(bookingDto.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(bookingDto.getHostApproved()).isFalse();

        // 2. Host accepts booking
        BookingResponseDto acceptedDto = bookingService.acceptBooking(bookingDto.getId(), host.getEmail());
        assertThat(acceptedDto.getHostApproved()).isTrue();
        assertThat(acceptedDto.getStatus()).isEqualTo(BookingStatus.PENDING);

        // 3. Simulate Payment entity creation (as if createOrder was called)
        Booking booking = bookingRepository.findById(bookingDto.getId()).orElseThrow();
        Payment payment = Payment.builder()
                .booking(booking)
                .razorpayOrderId("order_INTEG_12345")
                .amount(booking.getTotalPrice())
                .status(PaymentStatus.CREATED)
                .currency("INR")
                .build();
        paymentRepository.save(payment);

        // 4. Razorpay sends payment.captured webhook
        String webhookPayload = "{" +
                "\"event\": \"payment.captured\"," +
                "\"payload\": {" +
                "\"payment\": {" +
                "\"entity\": {" +
                "\"id\": \"pay_INTEG_SUCCESS\"," +
                "\"order_id\": \"order_INTEG_12345\"" +
                "}" +
                "}" +
                "}" +
                "}";

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.verifyWebhookSignature(eq(webhookPayload), eq("valid_sig"), anyString()))
                    .thenReturn(true);

            paymentService.processWebhookEvent(webhookPayload, "valid_sig");
        }

        // 5. Assert final state transitions
        Payment finalPayment = paymentRepository.findByRazorpayOrderId("order_INTEG_12345").orElseThrow();
        assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(finalPayment.getRazorpayPaymentId()).isEqualTo("pay_INTEG_SUCCESS");

        Booking finalBooking = bookingRepository.findById(bookingDto.getId()).orElseThrow();
        assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Edge Case: Webhook rejection when cryptographic signature is spoofed")
    void shouldRejectTamperedWebhookSignature() {
        String spoofedPayload = "{\"event\": \"payment.captured\"}";

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.verifyWebhookSignature(eq(spoofedPayload), eq("fake_sig"), anyString()))
                    .thenReturn(false);

            assertThatThrownBy(() -> paymentService.processWebhookEvent(spoofedPayload, "fake_sig"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid webhook signature");
        }
    }

    @Test
    @DisplayName("Edge Case: payment.failed event marks Payment as FAILED without confirming Booking")
    void shouldHandlePaymentFailedWebhookGracefully() {
        Booking booking = Booking.builder()
                .listing(listing)
                .guest(guest)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(32))
                .totalPrice(new BigDecimal("30000.00"))
                .status(BookingStatus.PENDING)
                .hostApproved(true)
                .build();
        bookingRepository.save(booking);

        Payment payment = Payment.builder()
                .booking(booking)
                .razorpayOrderId("order_DECLINED_999")
                .amount(booking.getTotalPrice())
                .status(PaymentStatus.CREATED)
                .currency("INR")
                .build();
        paymentRepository.save(payment);

        String failurePayload = "{" +
                "\"event\": \"payment.failed\"," +
                "\"payload\": {" +
                "\"payment\": {" +
                "\"entity\": {" +
                "\"id\": \"pay_DECLINED_000\"," +
                "\"order_id\": \"order_DECLINED_999\"" +
                "}" +
                "}" +
                "}" +
                "}";

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.verifyWebhookSignature(eq(failurePayload), eq("valid_sig"), anyString()))
                    .thenReturn(true);

            paymentService.processWebhookEvent(failurePayload, "valid_sig");
        }

        Payment updatedPayment = paymentRepository.findByRazorpayOrderId("order_DECLINED_999").orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        Booking unchangedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(unchangedBooking.getStatus()).isEqualTo(BookingStatus.PENDING); // Remains PENDING so user can retry
    }
}