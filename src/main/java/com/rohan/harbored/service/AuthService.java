package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.SignupRequestDto;
import com.rohan.harbored.dto.response.UserResponseDto;

public interface AuthService {
    UserResponseDto signup(SignupRequestDto request);
}