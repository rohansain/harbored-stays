package com.rohan.harbored.mapper;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    @DisplayName("Should correctly map User entity to UserResponseDto without exposing secrets")
    void shouldMapUserToUserResponseDto() {
        Instant now = Instant.now();
        User user = User.builder()
                .id(1L)
                .name("Rohan Developer")
                .email("rohan@harbored.com")
                .passwordHash("$2a$10$hashedStringExample")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .resetOtpCode("999888")
                .resetOtpExpiry(now.plusSeconds(300))
                .createdAt(now)
                .build();

        UserResponseDto dto = userMapper.toResponseDto(user);

        assertThat(dto).isNotNull();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Rohan Developer");
        assertThat(dto.getEmail()).isEqualTo("rohan@harbored.com");
        assertThat(dto.getRole()).isEqualTo(Role.HOST);
        assertThat(dto.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should return null when entity is null")
    void shouldReturnNullForNullEntity() {
        UserResponseDto dto = userMapper.toResponseDto(null);
        assertThat(dto).isNull();
    }
}