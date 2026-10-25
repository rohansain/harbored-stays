package com.rohan.harbored.controller;

import com.rohan.harbored.dto.request.CancelBookingRequestDto;
import com.rohan.harbored.dto.request.CreateBookingDto;
import com.rohan.harbored.dto.response.ApiResponse;
import com.rohan.harbored.dto.response.BookingResponseDto;
import com.rohan.harbored.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponseDto>> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateBookingDto request
    ) {
        BookingResponseDto response = bookingService.createBooking(userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Booking request created successfully with status PENDING"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponseDto>> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        BookingResponseDto response = bookingService.getBookingById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Booking details retrieved successfully"));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<BookingResponseDto> responses = bookingService.getMyBookings(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(responses, "User bookings retrieved successfully"));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<BookingResponseDto>> completeBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        BookingResponseDto response = bookingService.completeBooking(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Booking marked as COMPLETED successfully"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponseDto>> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CancelBookingRequestDto request
    ) {
        BookingResponseDto response = bookingService.cancelBooking(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking cancelled successfully"));
    }
}