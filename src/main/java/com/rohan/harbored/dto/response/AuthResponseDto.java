package com.rohan.harbored.dto.response;

import com.rohan.harbored.dto.response.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto {

    private String token;
    private String tokenType;
    private UserResponseDto user;
}