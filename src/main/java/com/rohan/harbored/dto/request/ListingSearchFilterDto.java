package com.rohan.harbored.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingSearchFilterDto {

    private String city;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum price cannot be negative")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum price cannot be negative")
    private BigDecimal maxPrice;
}