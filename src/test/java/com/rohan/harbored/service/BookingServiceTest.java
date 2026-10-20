package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.CreateBookingDto;
import com.rohan.harbored.dto.response.BookingResponseDto;
import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.BookingConflictException;
import com.rohan.harbored.mapper.BookingMapper;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.impl.BookingServiceImpl;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    @DisplayName("Should create booking with PENDING status and correct price when all conditions are valid")
    void shouldCreateBookingSuccessfully() {
        User host = User.builder().id(1L).email("host@harbored.com").build();
        User guest = User.builder().id(2L).email("guest@harbored.com").verificationStatus(VerificationStatus.VERIFIED).build();

        Listing listing = Listing.builder()
                .id(100L)
                .owner(host)
                .pricePerMonth(new BigDecimal("30000.00")) // 1000 per day
                .minStayMonths(1)
                .maxStayMonths(6)
                .build();

        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = start.plusDays(60); // 60 days stay

        CreateBookingDto request = CreateBookingDto.builder()
                .listingId(100L)
                .startDate(start)
                .endDate(end)
                .build();

        Booking savedBooking = Booking.builder()
                .id(500L)
                .listing(listing)
                .guest(guest)
                .startDate(start)
                .endDate(end)
                .totalPrice(new BigDecimal("60000.00"))
                .status(BookingStatus.PENDING)
                .build();

        when(userRepository.findByEmail("guest@harbored.com")).thenReturn(Optional.of(guest));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsOverlappingBooking(100L, BookingStatus.CONFIRMED, start, end)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toResponseDto(savedBooking)).thenReturn(
                BookingResponseDto.builder()
                        .id(500L)
                        .totalPrice(new BigDecimal("60000.00"))
                        .status(BookingStatus.PENDING)
                        .build()
        );

        BookingResponseDto result = bookingService.createBooking("guest@harbored.com", request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getTotalPrice()).isEqualTo(new BigDecimal("60000.00"));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should reject booking creation when guest is not VERIFIED")
    void shouldRejectBookingForUnverifiedGuest() {
        User unverifiedGuest = User.builder().id(2L).email("guest@harbored.com").verificationStatus(VerificationStatus.UNVERIFIED).build();
        CreateBookingDto request = CreateBookingDto.builder().listingId(100L).build();

        when(userRepository.findByEmail("guest@harbored.com")).thenReturn(Optional.of(unverifiedGuest));

        assertThatThrownBy(() -> bookingService.createBooking("guest@harbored.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only verified users can create bookings");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should reject booking creation when requested dates overlap an existing confirmed stay")
    void shouldRejectBookingWhenOverlapping() {
        User host = User.builder().id(1L).email("host@harbored.com").build();
        User guest = User.builder().id(2L).email("guest@harbored.com").verificationStatus(VerificationStatus.VERIFIED).build();

        Listing listing = Listing.builder()
                .id(100L)
                .owner(host)
                .pricePerMonth(new BigDecimal("30000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .build();

        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = start.plusDays(40);

        CreateBookingDto request = CreateBookingDto.builder()
                .listingId(100L)
                .startDate(start)
                .endDate(end)
                .build();

        when(userRepository.findByEmail("guest@harbored.com")).thenReturn(Optional.of(guest));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsOverlappingBooking(100L, BookingStatus.CONFIRMED, start, end)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking("guest@harbored.com", request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("The requested dates are no longer available for this listing");

        verify(bookingRepository, never()).save(any(Booking.class));
    }
}