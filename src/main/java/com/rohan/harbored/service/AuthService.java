package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.LoginRequestDto;
import com.rohan.harbored.dto.request.SignupRequestDto;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.harbored.dto.response.AuthResponseDto;

public interface AuthService {
    UserResponseDto signup(SignupRequestDto request);
    AuthResponseDto login(LoginRequestDto request);
}