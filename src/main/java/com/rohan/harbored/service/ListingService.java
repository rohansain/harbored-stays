package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.CreateListingDto;
import com.rohan.harbored.dto.request.UpdateListingDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ListingService {
    ListingResponseDto createListing(String userEmail, CreateListingDto request);
    ListingResponseDto getListingById(Long listingId);
    List<ListingResponseDto> getAllListings();
    ListingResponseDto updateListing(Long listingId, String userEmail, UpdateListingDto request);
    void deleteListing(Long listingId, String userEmail);
    ListingResponseDto uploadListingImages(Long listingId, String userEmail, List<MultipartFile> files);

}