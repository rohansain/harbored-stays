package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.CreatePaymentOrderRequestDto;
import com.rohan.harbored.dto.response.PaymentOrderResponseDto;

public interface PaymentService {
    PaymentOrderResponseDto createOrder(String guestEmail, CreatePaymentOrderRequestDto request);
}