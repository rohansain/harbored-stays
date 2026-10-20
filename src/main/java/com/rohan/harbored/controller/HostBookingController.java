package com.rohan.harbored.controller;

import com.rohan.harbored.dto.request.RejectBookingRequestDto;
import com.rohan.harbored.dto.response.ApiResponse;
import com.rohan.harbored.dto.response.BookingResponseDto;
import com.rohan.harbored.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/host/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
public class HostBookingController {

    private final BookingService bookingService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getPendingBookings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<BookingResponseDto> responses = bookingService.getHostPendingBookings(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(responses, "Host pending bookings retrieved successfully"));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<BookingResponseDto>> acceptBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        BookingResponseDto response = bookingService.acceptBooking(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Booking request accepted. Awaiting payment."));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<BookingResponseDto>> rejectBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) RejectBookingRequestDto request
    ) {
        BookingResponseDto response = bookingService.rejectBooking(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking request rejected and cancelled."));
    }
}