package com.rohan.harbored.dto.response;

import com.rohan.harbored.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponseDto {

    private Long id;
    private Long listingId;
    private String listingTitle;
    private String listingCity;
    private Long guestId;
    private String guestName;
    private String guestEmail;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private Boolean hostApproved;
    private Long version;
    private Instant createdAt;
}