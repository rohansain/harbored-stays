package com.rohan.harbored.service.impl;

import com.rohan.harbored.config.RedisConfig;
import com.rohan.harbored.dto.response.ListingAvailabilityResponseDto;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.ResourceNotFoundException;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.service.ListingAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingAvailabilityServiceImpl implements ListingAvailabilityService {

    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = RedisConfig.AVAILABILITY_CACHE,
            key = "'listing:' + #listingId + ':start:' + #startDate + ':end:' + #endDate",
            sync = true
    )
    public ListingAvailabilityResponseDto checkAvailability(Long listingId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Both startDate and endDate are required");
        }
        if (!startDate.isBefore(endDate)) {
            throw new BadRequestException("startDate must be strictly before endDate");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("startDate cannot be in the past");
        }

        log.info("Cache MISS for listingAvailability [listingId={}, startDate={}, endDate={}] - querying PostgreSQL",
                listingId, startDate, endDate);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        long monthsRequested = java.time.temporal.ChronoUnit.MONTHS.between(startDate, endDate);
        if (monthsRequested < listing.getMinStayMonths() || monthsRequested > listing.getMaxStayMonths()) {
            return ListingAvailabilityResponseDto.builder()
                    .listingId(listingId)
                    .startDate(startDate)
                    .endDate(endDate)
                    .available(false)
                    .message(String.format("Stay duration must be between %d and %d months",
                            listing.getMinStayMonths(), listing.getMaxStayMonths()))
                    .build();
        }

        boolean hasConflict = bookingRepository.existsByListingIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
                listingId,
                List.of(BookingStatus.CONFIRMED),
                endDate,
                startDate
        );

        return ListingAvailabilityResponseDto.builder()
                .listingId(listingId)
                .startDate(startDate)
                .endDate(endDate)
                .available(!hasConflict)
                .message(hasConflict
                        ? "Listing is already booked for the selected dates"
                        : "Listing is available for booking")
                .build();
    }
}