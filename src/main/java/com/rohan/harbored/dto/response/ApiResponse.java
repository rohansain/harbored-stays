package com.rohan.harbored.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private ApiError error;

    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation completed successfully");
    }

    public static <T> ApiResponse<T> failure(String message, String errorCode, Object details) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(new ApiError(errorCode, details))
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> failure(String message, String errorCode) {
        return failure(message, errorCode, null);
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiError {
        private String code;
        private Object details;

    }
}