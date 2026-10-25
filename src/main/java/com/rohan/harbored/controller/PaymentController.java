package com.rohan.harbored.controller;

import com.rohan.harbored.dto.request.CreatePaymentOrderRequestDto;
import com.rohan.harbored.dto.response.ApiResponse;
import com.rohan.harbored.dto.response.PaymentOrderResponseDto;
import com.rohan.harbored.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<PaymentOrderResponseDto>> createPaymentOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreatePaymentOrderRequestDto request
    ) {
        PaymentOrderResponseDto response = paymentService.createOrder(userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Razorpay payment order created successfully"));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader
    ) {
        paymentService.processWebhookEvent(rawPayload, signatureHeader);
        return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed successfully"));
    }
}