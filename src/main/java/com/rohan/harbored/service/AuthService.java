package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.ForgotPasswordRequestDto;
import com.rohan.harbored.dto.request.LoginRequestDto;
import com.rohan.harbored.dto.request.SignupRequestDto;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.dto.response.AuthResponseDto;
import com.rohan.harbored.dto.request.VerifyOtpRequestDto;
import com.rohan.harbored.dto.request.ResetPasswordRequestDto;

public interface AuthService {
    UserResponseDto signup(SignupRequestDto request);
    AuthResponseDto login(LoginRequestDto request);
    void initiatePasswordReset(ForgotPasswordRequestDto request);
    void verifyOtp(VerifyOtpRequestDto request);
    void resetPassword(ResetPasswordRequestDto request);

}