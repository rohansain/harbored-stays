package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.VerificationDecisionRequestDto;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationDocType;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.mapper.UserMapper;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.impl.VerificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    @Test
    @DisplayName("Should successfully upload document and transition status to PENDING")
    void shouldUploadDocumentSuccessfully() {
        MultipartFile file = mock(MultipartFile.class);
        User user = User.builder()
                .id(10L)
                .email("student@harbored.com")
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .build();

        when(userRepository.findByEmail("student@harbored.com")).thenReturn(Optional.of(user));
        when(documentStorageService.uploadVerificationDocument(file, 10L)).thenReturn("https://res.cloudinary.com/test.jpg");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponseDto(any(User.class))).thenReturn(UserResponseDto.builder()
                .id(10L)
                .verificationStatus(VerificationStatus.PENDING)
                .verificationDocUrl("https://res.cloudinary.com/test.jpg")
                .build());

        UserResponseDto response = verificationService.uploadVerificationDocument(
                "student@harbored.com",
                file,
                VerificationDocType.studentID
        );

        assertThat(response).isNotNull();
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(user.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(user.getVerificationDocUrl()).isEqualTo("https://res.cloudinary.com/test.jpg");
    }

    @Test
    @DisplayName("Should reject document upload if user is already VERIFIED")
    void shouldRejectUploadIfAlreadyVerified() {
        MultipartFile file = mock(MultipartFile.class);
        User user = User.builder()
                .id(10L)
                .email("verified@harbored.com")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        when(userRepository.findByEmail("verified@harbored.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> verificationService.uploadVerificationDocument(
                "verified@harbored.com",
                file,
                VerificationDocType.companyID
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User identity is already VERIFIED");
    }

    @Test
    @DisplayName("Should allow Admin to approve user verification transitioning status to VERIFIED")
    void shouldApproveVerification() {
        User user = User.builder()
                .id(12L)
                .verificationStatus(VerificationStatus.PENDING)
                .role(Role.GUEST)
                .build();

        VerificationDecisionRequestDto decision = VerificationDecisionRequestDto.builder()
                .approved(true)
                .build();

        when(userRepository.findById(12L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponseDto(any(User.class))).thenReturn(UserResponseDto.builder()
                .id(12L)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build());

        UserResponseDto result = verificationService.processVerificationDecision(12L, decision);

        assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(user.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        verify(userRepository).save(user);
    }
}