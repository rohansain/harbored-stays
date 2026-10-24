package com.rohan.harbored.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.rohan.harbored.dto.request.CreatePaymentOrderRequestDto;
import com.rohan.harbored.dto.response.PaymentOrderResponseDto;
import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Payment;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.PaymentStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.BookingConflictException;
import com.rohan.harbored.exception.ResourceNotFoundException;
import com.rohan.harbored.exception.UnauthorizedException;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.PaymentRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Override
    @Transactional
    public PaymentOrderResponseDto createOrder(String guestEmail, CreatePaymentOrderRequestDto request) {
        User currentUser = userRepository.findByEmail(guestEmail.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + guestEmail));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + request.getBookingId()));

        // 1. Authorize: only the booking's guest or an ADMIN can pay for it
        boolean isGuest = booking.getGuest().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isGuest && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to create a payment order for this booking");
        }

        // 2. Validate booking status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Payment order can only be created for PENDING bookings. Current status: " + booking.getStatus());
        }

        // 3. Enforce host approval gate
        if (!Boolean.TRUE.equals(booking.getHostApproved())) {
            throw new BadRequestException("Cannot initialize payment: The host has not accepted this booking request yet.");
        }

        // 4. Late-binding overlap check: ensure dates haven't been confirmed by another reservation
        boolean hasConfirmedOverlap = bookingRepository.existsOverlappingBooking(
                booking.getListing().getId(),
                BookingStatus.CONFIRMED,
                booking.getStartDate(),
                booking.getEndDate()
        );

        if (hasConfirmedOverlap) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setHostApproved(false);
            bookingRepository.save(booking);
            throw new BookingConflictException("Cannot proceed with payment: The requested dates have already been confirmed by another guest.");
        }

        // 5. Idempotency: reuse existing CREATED payment order if one was already issued
        Optional<Payment> existingPaymentOpt = paymentRepository.findByBookingId(booking.getId());
        if (existingPaymentOpt.isPresent()) {
            Payment existingPayment = existingPaymentOpt.get();
            if (existingPayment.getStatus() == PaymentStatus.CREATED) {
                log.info("Reusing existing Razorpay order {} for booking ID {}", existingPayment.getRazorpayOrderId(), booking.getId());
                long amountInPaise = existingPayment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
                return PaymentOrderResponseDto.builder()
                        .razorpayOrderId(existingPayment.getRazorpayOrderId())
                        .bookingId(booking.getId())
                        .amount(existingPayment.getAmount())
                        .amountInPaise(amountInPaise)
                        .currency(existingPayment.getCurrency())
                        .razorpayKeyId(razorpayKeyId)
                        .build();
            } else if (existingPayment.getStatus() == PaymentStatus.PAID) {
                throw new BadRequestException("This booking has already been paid for.");
            }
        }

        // 6. Convert INR to paise for Razorpay API (e.g. 25000.00 -> 2500000)
        long amountInPaise = booking.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue();

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_bkg_" + booking.getId());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            Payment payment = Payment.builder()
                    .booking(booking)
                    .razorpayOrderId(orderId)
                    .amount(booking.getTotalPrice())
                    .currency("INR")
                    .status(PaymentStatus.CREATED)
                    .build();

            paymentRepository.save(payment);
            log.info("Created Razorpay order {} for booking ID {}", orderId, booking.getId());

            return PaymentOrderResponseDto.builder()
                    .razorpayOrderId(orderId)
                    .bookingId(booking.getId())
                    .amount(booking.getTotalPrice())
                    .amountInPaise(amountInPaise)
                    .currency("INR")
                    .razorpayKeyId(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for booking ID {}: {}", booking.getId(), e.getMessage(), e);
            throw new BadRequestException("Unable to initialize payment gateway order: " + e.getMessage());
        }
    }
}