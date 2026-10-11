package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.VerificationDecisionRequestDto;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.entity.enums.VerificationDocType;
import org.springframework.web.multipart.MultipartFile;

public interface VerificationService {
    UserResponseDto uploadVerificationDocument(String userEmail, MultipartFile file, VerificationDocType docType);
    UserResponseDto processVerificationDecision(Long targetUserId, VerificationDecisionRequestDto request);
}