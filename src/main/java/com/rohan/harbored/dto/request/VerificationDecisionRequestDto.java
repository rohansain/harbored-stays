package com.rohan.harbored.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationDecisionRequestDto {

    @NotNull(message = "Decision approval flag must be provided")
    private Boolean approved;

    @Size(max = 250, message = "Rejection reason cannot exceed 250 characters")
    private String rejectionReason;
}