package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.ResetPasswordRequestDto;
import com.rohan.harbored.dto.request.VerifyOtpRequestDto;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.security.OtpRateLimiter;
import com.rohan.harbored.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceResetPasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpRateLimiter otpRateLimiter;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Should successfully verify valid and unexpired OTP")
    void shouldVerifyValidOtp() {
        VerifyOtpRequestDto request = VerifyOtpRequestDto.builder()
                .email("rohan@harbored.com")
                .otp("123456")
                .build();

        User user = User.builder()
                .email("rohan@harbored.com")
                .resetOtpCode("123456")
                .resetOtpExpiry(Instant.now().plusSeconds(300))
                .build();

        when(otpRateLimiter.isAllowed("rohan@harbored.com")).thenReturn(true);
        when(userRepository.findByEmail("rohan@harbored.com")).thenReturn(Optional.of(user));

        authService.verifyOtp(request);

        verify(otpRateLimiter).isAllowed("rohan@harbored.com");
    }

    @Test
    @DisplayName("Should throw BadRequestException when OTP has expired")
    void shouldThrowExceptionWhenOtpExpired() {
        VerifyOtpRequestDto request = VerifyOtpRequestDto.builder()
                .email("rohan@harbored.com")
                .otp("123456")
                .build();

        User user = User.builder()
                .email("rohan@harbored.com")
                .resetOtpCode("123456")
                .resetOtpExpiry(Instant.now().minusSeconds(10))
                .build();

        when(otpRateLimiter.isAllowed("rohan@harbored.com")).thenReturn(true);
        when(userRepository.findByEmail("rohan@harbored.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OTP has expired");
    }

    @Test
    @DisplayName("Should reset password and nullify OTP fields on success")
    void shouldResetPasswordAndWipeOtp() {
        ResetPasswordRequestDto request = ResetPasswordRequestDto.builder()
                .email("rohan@harbored.com")
                .otp("654321")
                .newPassword("NewSuperSecret123")
                .build();

        User user = User.builder()
                .email("rohan@harbored.com")
                .resetOtpCode("654321")
                .resetOtpExpiry(Instant.now().plusSeconds(300))
                .passwordHash("old_hash")
                .build();

        when(otpRateLimiter.isAllowed("rohan@harbored.com")).thenReturn(true);
        when(userRepository.findByEmail("rohan@harbored.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewSuperSecret123")).thenReturn("new_encoded_hash");

        authService.resetPassword(request);

        assertThat(user.getPasswordHash()).isEqualTo("new_encoded_hash");
        assertThat(user.getResetOtpCode()).isNull();
        assertThat(user.getResetOtpExpiry()).isNull();

        verify(userRepository).save(user);
        verify(otpRateLimiter).clear("rohan@harbored.com");
    }

    @Test
    @DisplayName("Should reject password reset when rate limit is exceeded")
    void shouldRejectWhenRateLimitExceeded() {
        ResetPasswordRequestDto request = ResetPasswordRequestDto.builder()
                .email("rohan@harbored.com")
                .otp("654321")
                .newPassword("NewSuperSecret123")
                .build();

        when(otpRateLimiter.isAllowed("rohan@harbored.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Too many verification attempts");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}