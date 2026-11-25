package com.rohan.harbored.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingAvailabilityResponseDto {

    private Long listingId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean available;
    private String message;
}