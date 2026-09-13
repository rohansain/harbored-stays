package com.rohan.harbored.service.impl;

import com.rohan.harbored.dto.request.SignupRequestDto;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.mapper.UserMapper;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponseDto signup(SignupRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toResponseDto(savedUser);
    }
}