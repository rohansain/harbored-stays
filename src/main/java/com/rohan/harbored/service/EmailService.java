package com.rohan.harbored.service;

public interface EmailService {
    void sendOtpEmail(String recipientEmail, String otpCode);
}
