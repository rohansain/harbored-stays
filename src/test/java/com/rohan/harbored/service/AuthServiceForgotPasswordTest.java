package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.ForgotPasswordRequestDto;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceForgotPasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Should generate 6-digit OTP, set 10-minute expiry, and dispatch email when user exists")
    void shouldInitiatePasswordResetForValidUser() {
        ForgotPasswordRequestDto request = ForgotPasswordRequestDto.builder()
                .email("rohan@harbored.com")
                .build();

        User user = User.builder()
                .id(1L)
                .email("rohan@harbored.com")
                .build();

        when(userRepository.findByEmail("rohan@harbored.com")).thenReturn(Optional.of(user));

        authService.initiatePasswordReset(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getResetOtpCode()).isNotNull().hasSize(6);
        assertThat(capturedUser.getResetOtpExpiry()).isAfter(Instant.now());

        verify(emailService).sendOtpEmail(eq("rohan@harbored.com"), eq(capturedUser.getResetOtpCode()));
    }

    @Test
    @DisplayName("Should silently return and never trigger email dispatch when user does not exist")
    void shouldSilentlyIgnoreNonExistentUser() {
        ForgotPasswordRequestDto request = ForgotPasswordRequestDto.builder()
                .email("ghost@harbored.com")
                .build();

        when(userRepository.findByEmail("ghost@harbored.com")).thenReturn(Optional.empty());

        authService.initiatePasswordReset(request);

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendOtpEmail(anyString(), anyString());
    }
}