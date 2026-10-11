package com.rohan.harbored.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryDocumentStorageServiceImpl implements DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryDocumentStorageServiceImpl.class);
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    private final Cloudinary cloudinary;

    @Override
    public String uploadVerificationDocument(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Verification document file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file format. Allowed formats: JPEG, PNG, PDF");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "harbored/verifications/user_" + userId,
                    "resource_type", "auto"
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Document successfully uploaded to Cloudinary for user ID: {}", userId);
            return secureUrl;
        } catch (IOException e) {
            log.error("Cloudinary upload failed for user ID: {}", userId, e);
            throw new BadRequestException("Failed to upload verification document to cloud storage");
        }
    }
}