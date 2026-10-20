package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.CreateBookingDto;
import com.rohan.harbored.dto.response.BookingResponseDto;

import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(String guestEmail, CreateBookingDto request);
    BookingResponseDto getBookingById(Long bookingId, String userEmail);
    List<BookingResponseDto> getMyBookings(String guestEmail);
}