package com.rohan.harbored.service.impl;

import com.rohan.harbored.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String recipientEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setFrom("security@harbored.com");
        message.setSubject("Your Harbored Password Reset OTP");
        message.setText("Hello,\n\n"
                + "You requested a password reset for your Harbored account.\n"
                + "Your single-use 6-digit verification code is:\n\n"
                + "   " + otpCode + "\n\n"
                + "This code will expire in exactly 10 minutes.\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Best,\n"
                + "The Harbored Security Team");

        try {
            mailSender.send(message);
            log.info("Password reset OTP successfully dispatched to {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", recipientEmail, e.getMessage());
        }
    }
}