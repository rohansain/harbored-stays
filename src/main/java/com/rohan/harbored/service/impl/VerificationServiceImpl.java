package com.rohan.harbored.service.impl;

import com.rohan.harbored.dto.request.VerificationDecisionRequestDto;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.VerificationDocType;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.ResourceNotFoundException;
import com.rohan.harbored.mapper.UserMapper;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.DocumentStorageService;
import com.rohan.harbored.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationServiceImpl.class);

    private final UserRepository userRepository;
    private final DocumentStorageService documentStorageService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponseDto uploadVerificationDocument(String userEmail, MultipartFile file, VerificationDocType docType) {
        User user = userRepository.findByEmail(userEmail.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (user.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new BadRequestException("User identity is already VERIFIED. Additional submissions are not permitted.");
        }

        String uploadedUrl = documentStorageService.uploadVerificationDocument(file, user.getId());

        user.setVerificationDocType(docType);
        user.setVerificationDocUrl(uploadedUrl);
        user.setVerificationStatus(VerificationStatus.PENDING);

        User updatedUser = userRepository.save(user);
        log.info("User ID {} status transitioned to PENDING verification", user.getId());

        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto processVerificationDecision(Long targetUserId, VerificationDecisionRequestDto request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with ID: " + targetUserId));

        if (user.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new BadRequestException("Cannot process verification decision for user not in PENDING state. Current status: " + user.getVerificationStatus());
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            user.setVerificationStatus(VerificationStatus.VERIFIED);
            log.info("Admin approved verification for User ID {}", targetUserId);
        } else {
            user.setVerificationStatus(VerificationStatus.UNVERIFIED);
            log.warn("Admin rejected verification for User ID {}. Reason: {}", targetUserId, request.getRejectionReason());
        }

        User savedUser = userRepository.save(user);
        return userMapper.toResponseDto(savedUser);
    }
}