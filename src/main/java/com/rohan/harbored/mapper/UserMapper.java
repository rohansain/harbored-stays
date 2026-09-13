package com.rohan.harbored.mapper;

import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDto toResponseDto(User user) {
        if (user == null) {
            return null;
        }

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .verificationStatus(user.getVerificationStatus())
                .verificationDocType(user.getVerificationDocType())
                .createdAt(user.getCreatedAt())
                .build();
    }
}