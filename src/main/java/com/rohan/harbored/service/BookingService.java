package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.CancelBookingRequestDto;
import com.rohan.harbored.dto.request.CreateBookingDto;
import com.rohan.harbored.dto.request.RejectBookingRequestDto;
import com.rohan.harbored.dto.response.BookingResponseDto;

import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(String guestEmail, CreateBookingDto request);
    BookingResponseDto getBookingById(Long bookingId, String userEmail);
    List<BookingResponseDto> getMyBookings(String guestEmail);
    List<BookingResponseDto> getHostPendingBookings(String hostEmail);
    BookingResponseDto acceptBooking(Long bookingId, String hostEmail);
    BookingResponseDto rejectBooking(Long bookingId, String hostEmail, RejectBookingRequestDto request);
    BookingResponseDto completeBooking(Long bookingId, String callerEmail);
    BookingResponseDto cancelBooking(Long bookingId, String callerEmail, CancelBookingRequestDto request);
    int markExpiredBookingsAsCompleted();
}