package com.rohan.harbored.service.impl;

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
import com.rohan.harbored.exception.ResourceNotFoundException;
import com.rohan.harbored.exception.UnauthorizedException;
import com.rohan.harbored.mapper.BookingMapper;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final BigDecimal DAYS_PER_MONTH = BigDecimal.valueOf(30);

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponseDto createBooking(String guestEmail, CreateBookingDto request) {
        User guest = userRepository.findByEmail(guestEmail.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + guestEmail));

        if (guest.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BadRequestException("Only verified users can create bookings. Current status: " + guest.getVerificationStatus());
        }

        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + request.getListingId()));

        if (listing.getOwner().getId().equals(guest.getId())) {
            throw new BadRequestException("Hosts cannot book their own property");
        }

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (!startDate.isBefore(endDate)) {
            throw new BadRequestException("Start date must be strictly before end date");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Start date cannot be in the past");
        }

        long stayDays = ChronoUnit.DAYS.between(startDate, endDate);
        long minDaysAllowed = (long) listing.getMinStayMonths() * 30;
        long maxDaysAllowed = (long) listing.getMaxStayMonths() * 30;

        if (stayDays < minDaysAllowed) {
            throw new BadRequestException("Stay duration (" + stayDays + " days) is less than listing minimum stay requirement (" + minDaysAllowed + " days)");
        }

        if (stayDays > maxDaysAllowed) {
            throw new BadRequestException("Stay duration (" + stayDays + " days) exceeds listing maximum stay requirement (" + maxDaysAllowed + " days)");
        }

        // 1. Guard against duplicate pending requests by the same guest
        boolean hasExistingPending = bookingRepository.existsOverlappingBookingForGuest(
                listing.getId(),
                guest.getId(),
                BookingStatus.PENDING,
                startDate,
                endDate
        );

        if (hasExistingPending) {
            throw new BookingConflictException("You already have a pending booking request for these dates on this listing.");
        }

        // 2. Guard against dates already locked by a confirmed booking
        boolean hasConfirmedOverlap = bookingRepository.existsOverlappingBooking(
                listing.getId(),
                BookingStatus.CONFIRMED,
                startDate,
                endDate
        );

        if (hasConfirmedOverlap) {
            throw new BookingConflictException("The requested dates are no longer available for this listing");
        }

        BigDecimal dailyRate = listing.getPricePerMonth().divide(DAYS_PER_MONTH, 4, RoundingMode.HALF_UP);
        BigDecimal totalPrice = dailyRate.multiply(BigDecimal.valueOf(stayDays)).setScale(2, RoundingMode.HALF_UP);

        Booking booking = Booking.builder()
                .listing(listing)
                .guest(guest)
                .startDate(startDate)
                .endDate(endDate)
                .totalPrice(totalPrice)
                .status(BookingStatus.PENDING)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        return bookingMapper.toResponseDto(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(Long bookingId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        boolean isGuest = booking.getGuest().getId().equals(currentUser.getId());
        boolean isOwner = booking.getListing().getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isGuest && !isOwner && !isAdmin) {
            throw new UnauthorizedException("You do not have permission to view this booking");
        }

        return bookingMapper.toResponseDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings(String guestEmail) {
        User guest = userRepository.findByEmail(guestEmail.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + guestEmail));

        return bookingRepository.findByGuestIdOrderByCreatedAtDesc(guest.getId()).stream()
                .map(bookingMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}