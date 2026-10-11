package com.rohan.harbored.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentStorageService {
    String uploadVerificationDocument(MultipartFile file, Long userId);
}