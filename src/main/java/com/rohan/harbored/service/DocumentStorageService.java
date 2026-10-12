package com.rohan.harbored.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentStorageService {
    String uploadVerificationDocument(MultipartFile file, Long userId);
    List<String> uploadListingImages(List<MultipartFile> files, Long listingId);
}