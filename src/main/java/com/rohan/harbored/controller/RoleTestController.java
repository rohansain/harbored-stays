package com.rohan.harbored.controller;

import com.rohan.harbored.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class RoleTestController {

    @GetMapping("/guest")
    @PreAuthorize("hasRole('GUEST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> guestEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Guest dashboard data accessed"));
    }

    @GetMapping("/host")
    @PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> hostEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Host property management data accessed"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Admin configuration data accessed"));
    }
}