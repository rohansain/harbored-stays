package com.rohan.harbored.controller;

import com.rohan.harbored.dto.response.ApiResponse;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.VerificationDocType;
import com.rohan.harbored.exception.ResourceNotFoundException;
import com.rohan.harbored.mapper.UserMapper;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final VerificationService verificationService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(ApiResponse.success(userMapper.toResponseDto(user)));
    }

    @PostMapping(value = "/verify/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponseDto>> uploadVerificationDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam("docType") VerificationDocType docType
    ) {
        UserResponseDto updatedUser = verificationService.uploadVerificationDocument(
                userDetails.getUsername(),
                file,
                docType
        );
        return ResponseEntity.ok(ApiResponse.success(
                updatedUser,
                "Verification document uploaded successfully. Status is now PENDING."
        ));
    }
}