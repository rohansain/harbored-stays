package com.rohan.harbored.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentOrderResponseDto {

    private String razorpayOrderId;
    private Long bookingId;
    private BigDecimal amount;
    private Long amountInPaise;
    private String currency;
    private String razorpayKeyId;
}
