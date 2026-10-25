package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.CancelBookingRequestDto;
import com.rohan.harbored.dto.response.BookingResponseDto;
import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.exception.BadRequestException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingLifecycleCompletionTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    @DisplayName("Should successfully mark CONFIRMED booking as COMPLETED when end date has passed")
    void shouldCompleteBookingWhenEndDatePassed() {
        User guest = User.builder().id(1L).email("guest@harbored.com").role(Role.GUEST).build();
        Listing listing = Listing.builder().id(10L).owner(User.builder().id(2L).build()).build();

        Booking booking = Booking.builder()
                .id(100L)
                .guest(guest)
                .listing(listing)
                .status(BookingStatus.CONFIRMED)
                .startDate(LocalDate.now().minusDays(40))
                .endDate(LocalDate.now().minusDays(5)) // Stay completed 5 days ago
                .build();

        when(userRepository.findByEmail("guest@harbored.com")).thenReturn(Optional.of(guest));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toResponseDto(booking)).thenReturn(BookingResponseDto.builder().id(100L).status(BookingStatus.COMPLETED).build());

        BookingResponseDto result = bookingService.completeBooking(100L, "guest@harbored.com");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should reject completion when stay end date has not passed yet")
    void shouldRejectCompletionBeforeEndDate() {
        User guest = User.builder().id(1L).email("guest@harbored.com").role(Role.GUEST).build();
        Listing listing = Listing.builder().id(10L).owner(User.builder().id(2L).build()).build();

        Booking booking = Booking.builder()
                .id(100L)
                .guest(guest)
                .listing(listing)
                .status(BookingStatus.CONFIRMED)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20)) // Stay still ongoing
                .build();

        when(userRepository.findByEmail("guest@harbored.com")).thenReturn(Optional.of(guest));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.completeBooking(100L, "guest@harbored.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Stay end date")
                .hasMessageContaining("has not passed yet");
    }

    @Test
    @DisplayName("Should successfully cancel CONFIRMED booking and record audit reason")
    void shouldCancelConfirmedBooking() {
        User guest = User.builder().id(1L).email("guest@harbored.com").role(Role.GUEST).build();
        Listing listing = Listing.builder().id(10L).owner(User.builder().id(2L).build()).build();

        Booking booking = Booking.builder()
                .id(100L)
                .guest(guest)
                .listing(listing)
                .status(BookingStatus.CONFIRMED)
                .hostApproved(true)
                .build();

        CancelBookingRequestDto request = CancelBookingRequestDto.builder()
                .reason("Internship relocation canceled by employer")
                .build();

        when(userRepository.findByEmail("guest@harbored.com")).thenReturn(Optional.of(guest));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toResponseDto(booking)).thenReturn(BookingResponseDto.builder().id(100L).status(BookingStatus.CANCELLED).build());

        BookingResponseDto result = bookingService.cancelBooking(100L, "guest@harbored.com", request);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getHostApproved()).isFalse();
        verify(bookingRepository).save(booking);
    }
}