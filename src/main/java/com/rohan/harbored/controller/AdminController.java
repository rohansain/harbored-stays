package com.rohan.harbored.controller;

import com.rohan.harbored.dto.request.VerificationDecisionRequestDto;
import com.rohan.harbored.dto.response.ApiResponse;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final VerificationService verificationService;

    @PostMapping("/verifications/{userId}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> processVerificationDecision(
            @PathVariable Long userId,
            @Valid @RequestBody VerificationDecisionRequestDto request
    ) {
        UserResponseDto updatedUser = verificationService.processVerificationDecision(userId, request);
        return ResponseEntity.ok(ApiResponse.success(
                updatedUser,
                "Verification decision processed successfully"
        ));
    }
}