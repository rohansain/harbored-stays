package com.rohan.harbored.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Payment;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.PaymentStatus;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.PaymentRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookIdempotencyTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayWebhookSecret", "test_secret_123");
    }

    @Test
    @DisplayName("Should update Payment to PAID and Booking to CONFIRMED on first webhook arrival")
    void shouldConfirmBookingOnFirstWebhook() {
        String payload = "{" +
                "\"event\": \"payment.captured\"," +
                "\"payload\": {" +
                "\"payment\": {" +
                "\"entity\": {" +
                "\"id\": \"pay_LIVE_777\"," +
                "\"order_id\": \"order_ABC_123\"" +
                "}" +
                "}" +
                "}" +
                "}";

        String signature = "valid_sig";

        Booking booking = Booking.builder()
                .id(10L)
                .status(BookingStatus.PENDING)
                .build();

        Payment payment = Payment.builder()
                .id(100L)
                .booking(booking)
                .razorpayOrderId("order_ABC_123")
                .amount(new BigDecimal("25000.00"))
                .status(PaymentStatus.CREATED)
                .build();

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.verifyWebhookSignature(eq(payload), eq(signature), eq("test_secret_123")))
                    .thenReturn(true);

            when(paymentRepository.findByRazorpayOrderId("order_ABC_123")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
            when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

            paymentService.processWebhookEvent(payload, signature);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getRazorpayPaymentId()).isEqualTo("pay_LIVE_777");
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

            verify(paymentRepository, times(1)).save(payment);
            verify(bookingRepository, times(1)).save(booking);
        }
    }

    @Test
    @DisplayName("Should be idempotent: duplicate webhook delivery should make zero database updates")
    void shouldHandleDuplicateWebhookIdempotently() {
        String payload = "{" +
                "\"event\": \"payment.captured\"," +
                "\"payload\": {" +
                "\"payment\": {" +
                "\"entity\": {" +
                "\"id\": \"pay_LIVE_777\"," +
                "\"order_id\": \"order_ABC_123\"" +
                "}" +
                "}" +
                "}" +
                "}";

        String signature = "valid_sig";

        Booking alreadyConfirmedBooking = Booking.builder()
                .id(10L)
                .status(BookingStatus.CONFIRMED)
                .build();

        Payment alreadyPaidPayment = Payment.builder()
                .id(100L)
                .booking(alreadyConfirmedBooking)
                .razorpayOrderId("order_ABC_123")
                .razorpayPaymentId("pay_LIVE_777")
                .status(PaymentStatus.PAID)
                .build();

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.verifyWebhookSignature(eq(payload), eq(signature), eq("test_secret_123")))
                    .thenReturn(true);

            when(paymentRepository.findByRazorpayOrderId("order_ABC_123")).thenReturn(Optional.of(alreadyPaidPayment));

            // Execute duplicate webhook call
            paymentService.processWebhookEvent(payload, signature);

            // Assert zero further updates to repository
            verify(paymentRepository, never()).save(any(Payment.class));
            verify(bookingRepository, never()).save(any(Booking.class));
        }
    }
}