package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.RejectBookingRequestDto;
import com.rohan.harbored.dto.response.BookingResponseDto;
import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.UnauthorizedException;
import com.rohan.harbored.mapper.BookingMapper;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostBookingFlowTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    @DisplayName("Host should reject pending booking transitioning status to CANCELLED")
    void shouldRejectPendingBooking() {
        User host = User.builder().id(10L).email("host@harbored.com").role(Role.HOST).build();
        Listing listing = Listing.builder().id(20L).owner(host).build();
        Booking booking = Booking.builder()
                .id(100L)
                .listing(listing)
                .status(BookingStatus.PENDING)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(35))
                .build();

        RejectBookingRequestDto request = RejectBookingRequestDto.builder()
                .reason("Dates clash with renovation")
                .build();

        when(userRepository.findByEmail("host@harbored.com")).thenReturn(Optional.of(host));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toResponseDto(booking)).thenReturn(BookingResponseDto.builder().id(100L).status(BookingStatus.CANCELLED).build());

        BookingResponseDto result = bookingService.rejectBooking(100L, "host@harbored.com", request);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Non-owner host should be rejected when trying to accept booking")
    void shouldBlockUnauthorizedHost() {
        User actualOwner = User.builder().id(10L).email("owner@harbored.com").role(Role.HOST).build();
        User intruderHost = User.builder().id(99L).email("intruder@harbored.com").role(Role.HOST).build();
        Listing listing = Listing.builder().id(20L).owner(actualOwner).build();
        Booking booking = Booking.builder().id(100L).listing(listing).status(BookingStatus.PENDING).build();

        when(userRepository.findByEmail("intruder@harbored.com")).thenReturn(Optional.of(intruderHost));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.acceptBooking(100L, "intruder@harbored.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You are not authorized");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Host cannot reject a booking that is already CANCELLED")
    void shouldBlockRejectingNonPendingBooking() {
        User host = User.builder().id(10L).email("host@harbored.com").role(Role.HOST).build();
        Listing listing = Listing.builder().id(20L).owner(host).build();
        Booking booking = Booking.builder().id(100L).listing(listing).status(BookingStatus.CANCELLED).build();

        when(userRepository.findByEmail("host@harbored.com")).thenReturn(Optional.of(host));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.rejectBooking(100L, "host@harbored.com", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PENDING bookings can be rejected");
    }
}