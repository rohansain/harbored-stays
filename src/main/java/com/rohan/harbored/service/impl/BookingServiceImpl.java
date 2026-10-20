package com.rohan.harbored.service.impl;

import com.rohan.harbored.dto.request.CreateBookingDto;
import com.rohan.harbored.dto.request.RejectBookingRequestDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final BigDecimal DAYS_PER_MONTH = BigDecimal.valueOf(30);

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponseDto createBooking(String guestEmail, CreateBookingDto request) {
        User guest = getUserByEmail(guestEmail);

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
        User currentUser = getUserByEmail(userEmail);

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
        User guest = getUserByEmail(guestEmail);

        return bookingRepository. findByGuestIdOrderByCreatedAtDesc(guest.getId()).stream()
                .map(bookingMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getHostPendingBookings(String hostEmail) {
        User host = getUserByEmail(hostEmail);

        return bookingRepository.findByHostIdAndStatus(host.getId(), BookingStatus.PENDING).stream()
                .map(bookingMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponseDto acceptBooking(Long bookingId, String hostEmail) {
        User host = getUserByEmail(hostEmail);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        validateHostOwnership(booking, host);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only PENDING bookings can be accepted. Current status: " + booking.getStatus());
        }

        boolean hasConfirmedOverlap = bookingRepository.existsOverlappingBooking(
                booking.getListing().getId(),
                BookingStatus.CONFIRMED,
                booking.getStartDate(),
                booking.getEndDate()
        );

        if (hasConfirmedOverlap) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            throw new BookingConflictException("Cannot accept booking: Dates have already been confirmed for another reservation.");
        }

        // Host approval acknowledges the request, remaining PENDING until payment confirmation (Day 25)
        log.info("Host {} accepted booking request ID {}. Awaiting guest payment settlement.", host.getEmail(), booking.getId());
        return bookingMapper.toResponseDto(booking);
    }

    @Override
    @Transactional
    public BookingResponseDto rejectBooking(Long bookingId, String hostEmail, RejectBookingRequestDto request) {
        User host = getUserByEmail(hostEmail);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        validateHostOwnership(booking, host);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only PENDING bookings can be rejected. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);

        log.info("Host {} rejected booking ID {}. Reason: {}", host.getEmail(), booking.getId(),
                request != null ? request.getReason() : "None provided");

        return bookingMapper.toResponseDto(cancelledBooking);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private void validateHostOwnership(Booking booking, User host) {
        boolean isOwner = booking.getListing().getOwner().getId().equals(host.getId());
        boolean isAdmin = host.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to manage bookings for this property");
        }
    }
}