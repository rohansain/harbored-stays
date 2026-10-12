package com.rohan.harbored.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateListingDto {

    @NotBlank(message = "Listing title is required")
    @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 4000, message = "Description must be between 10 and 4000 characters")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location cannot exceed 200 characters")
    private String location;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotNull(message = "Monthly price is required")
    @DecimalMin(value = "500.00", message = "Minimum monthly price must be at least 500.00")
    private BigDecimal pricePerMonth;

    @NotNull(message = "Minimum stay months is required")
    @Min(value = 1, message = "Minimum stay must be at least 1 month")
    private Integer minStayMonths;

    @NotNull(message = "Maximum stay months is required")
    @Max(value = 24, message = "Maximum stay cannot exceed 24 months")
    private Integer maxStayMonths;

    private List<String> images;
}