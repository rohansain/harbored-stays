package com.rohan.harbored.mapper;

import com.rohan.harbored.dto.response.BookingResponseDto;
import com.rohan.harbored.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponseDto toResponseDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        return BookingResponseDto.builder()
                .id(booking.getId())
                .listingId(booking.getListing() != null ? booking.getListing().getId() : null)
                .listingTitle(booking.getListing() != null ? booking.getListing().getTitle() : null)
                .listingCity(booking.getListing() != null ? booking.getListing().getCity() : null)
                .guestId(booking.getGuest() != null ? booking.getGuest().getId() : null)
                .guestName(booking.getGuest() != null ? booking.getGuest().getName() : null)
                .guestEmail(booking.getGuest() != null ? booking.getGuest().getEmail() : null)
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .hostApproved(booking.getHostApproved())
                .version(booking.getVersion())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}