package com.rohan.harbored.service;

import com.rohan.harbored.dto.response.ListingAvailabilityResponseDto;

import java.time.LocalDate;

public interface ListingAvailabilityService {

    ListingAvailabilityResponseDto checkAvailability(Long listingId, LocalDate startDate, LocalDate endDate);
}