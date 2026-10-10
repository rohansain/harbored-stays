package com.rohan.harbored.controller;

import com.rohan.harbored.dto.request.ForgotPasswordRequestDto;
import com.rohan.harbored.dto.request.LoginRequestDto;
import com.rohan.harbored.dto.request.SignupRequestDto;
import com.rohan.harbored.dto.response.ApiResponse;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.harbored.dto.response.AuthResponseDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    public final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponseDto>> signup(@Valid @RequestBody SignupRequestDto request) {
        UserResponseDto createdUser = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdUser, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto authResponse = authService.login(request);
        return ResponseEntity
                .ok(ApiResponse.success(authResponse, "Authentication successful"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.initiatePasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(
                null,
                "If an account exists with this email, an OTP has been sent."
        ));
    }
}
