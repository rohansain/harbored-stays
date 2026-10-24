package com.rohan.harbored.dto.request;

import com.rohan.harbored.entity.Booking;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentOrderRequestDto {
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
}
