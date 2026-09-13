package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.SignupRequestDto;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.mapper.UserMapper;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Should register a new user successfully")
    void shouldSignupSuccessfully() {
        SignupRequestDto request = SignupRequestDto.builder()
                .name("Rohan Backend")
                .email("rohan@harbored.com")
                .password("SecuredPassword123")
                .role(Role.GUEST)
                .build();

        User savedUser = User.builder()
                .id(1L)
                .name("Rohan Backend")
                .email("rohan@harbored.com")
                .passwordHash("encoded_bcrypt_hash")
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .createdAt(Instant.now())
                .build();

        UserResponseDto responseDto = UserResponseDto.builder()
                .id(1L)
                .name("Rohan Backend")
                .email("rohan@harbored.com")
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .createdAt(savedUser.getCreatedAt())
                .build();

        when(userRepository.existsByEmail("rohan@harbored.com")).thenReturn(false);
        when(passwordEncoder.encode("SecuredPassword123")).thenReturn("encoded_bcrypt_hash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponseDto(savedUser)).thenReturn(responseDto);

        UserResponseDto result = authService.signup(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("rohan@harbored.com");
        assertThat(result.getRole()).isEqualTo(Role.GUEST);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        SignupRequestDto request = SignupRequestDto.builder()
                .name("Rohan Backend")
                .email("rohan@harbored.com")
                .password("SecuredPassword123")
                .role(Role.GUEST)
                .build();

        when(userRepository.existsByEmail("rohan@harbored.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}