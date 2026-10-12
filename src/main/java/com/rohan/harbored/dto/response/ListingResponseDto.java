package com.rohan.harbored.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListingResponseDto {

    private Long id;
    private String title;
    private String description;
    private String location;
    private String city;
    private BigDecimal pricePerMonth;
    private Integer minStayMonths;
    private Integer maxStayMonths;
    private List<String> images;
    private Double averageRating;
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private Instant createdAt;
}