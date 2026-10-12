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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryDocumentStorageServiceImpl implements DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryDocumentStorageServiceImpl.class);

    private static final List<String> ALLOWED_DOC_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    private final Cloudinary cloudinary;

    @Override
    public String uploadVerificationDocument(MultipartFile file, Long userId) {
        validateFile(file, ALLOWED_DOC_TYPES);

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

    @Override
    public List<String> uploadListingImages(List<MultipartFile> files, Long listingId) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one image file must be provided");
        }

        if (files.size() > 10) {
            throw new BadRequestException("Cannot upload more than 10 images at once");
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            validateFile(file, ALLOWED_IMAGE_TYPES);

            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                        "folder", "harbored/listings/listing_" + listingId,
                        "resource_type", "image"
                ));

                String secureUrl = (String) uploadResult.get("secure_url");
                uploadedUrls.add(secureUrl);
            } catch (IOException e) {
                log.error("Cloudinary upload failed for listing ID: {}", listingId, e);
                throw new BadRequestException("Failed to upload listing image to cloud storage: " + file.getOriginalFilename());
            }
        }

        log.info("Successfully uploaded {} images for listing ID: {}", uploadedUrls.size(), listingId);
        return uploadedUrls;
    }

    private void validateFile(MultipartFile file, List<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file format. Allowed formats: " + allowedTypes);
        }
    }
}