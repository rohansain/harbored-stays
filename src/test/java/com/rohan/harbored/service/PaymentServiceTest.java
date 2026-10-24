package com.rohan.harbored.service;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.rohan.harbored.dto.request.CreatePaymentOrderRequestDto;
import com.rohan.harbored.dto.response.PaymentOrderResponseDto;
import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.Payment;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.PaymentStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.UnauthorizedException;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.PaymentRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.impl.PaymentServiceImpl;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

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

    @Test
    @DisplayName("Should successfully create a Razorpay order when booking is PENDING and hostApproved is TRUE")
    void shouldCreatePaymentOrderWhenHostApproved() throws Exception {
        User guest = User.builder().id(1L).email("guest@harbored.com").role(Role.GUEST).build();
        Listing listing = Listing.builder().id(10L).build();
        Booking booking = Booking.builder()
                .id(100L)
                .guest(guest)
                .listing(listing)
                .status(BookingStatus.PENDING)
                .hostApproved(true)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(32))
                .totalPrice(new BigDecimal("20000.00"))
                .build();

        CreatePaymentOrderRequestDto request = CreatePaymentOrderRequestDto.builder()
                .bookingId(100L)
                .build();

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_RZP_123456");

        OrderClient mockOrders = mock(OrderClient.class);
        when(mockOrders.create(any(JSONObject.class))).thenReturn(mockOrder);
        razorpayClient.orders = mockOrders;

        when(userRepository.findByEmail("guest@harbored.com")).thenReturn(Optional.of(guest));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.existsOverlappingBooking(10L, BookingStatus.CONFIRMED, booking.getStartDate(), booking.getEndDate())).thenReturn(false);
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.empty());

        PaymentOrderResponseDto response = paymentService.createOrder("guest@harbored.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_RZP_123456");
        assertThat(response.getAmountInPaise()).isEqualTo(2000000L);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should reject order creation when booking is PENDING but hostApproved is FALSE")
    void shouldRejectOrderCreationWhenNotHostApproved() {
        User guest = User.builder().id(1L).email("guest@harbored.com").role(Role.GUEST).build();
        Booking booking = Booking.builder()
                .id(100L)
                .guest(guest)
                .status(BookingStatus.PENDING)
                .hostApproved(false)
                .build();

        CreatePaymentOrderRequestDto request = CreatePaymentOrderRequestDto.builder()
                .bookingId(100L)
                .build();

        when(userRepository.findByEmail("guest@harbored.com")).thenReturn(Optional.of(guest));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createOrder("guest@harbored.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("The host has not accepted this booking request yet");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation if user is not the booking guest")
    void shouldRejectUnauthorizedCaller() {
        User guest = User.builder().id(1L).email("guest@harbored.com").role(Role.GUEST).build();
        User intruder = User.builder().id(2L).email("intruder@harbored.com").role(Role.GUEST).build();
        Booking booking = Booking.builder().id(100L).guest(guest).build();

        CreatePaymentOrderRequestDto request = CreatePaymentOrderRequestDto.builder().bookingId(100L).build();

        when(userRepository.findByEmail("intruder@harbored.com")).thenReturn(Optional.of(intruder));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createOrder("intruder@harbored.com", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You are not authorized");
    }
}