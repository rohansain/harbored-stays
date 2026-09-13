package com.rohan.harbored.dto.response;

import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationDocType;
import com.rohan.harbored.entity.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private VerificationStatus verificationStatus;
    private VerificationDocType verificationDocType;
    private Instant createdAt;
}