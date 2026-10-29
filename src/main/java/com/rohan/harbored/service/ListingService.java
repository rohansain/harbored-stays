package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.CreateListingDto;
import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.dto.response.PagedResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ListingService {
    ListingResponseDto createListing(String ownerEmail, CreateListingDto request);
    ListingResponseDto getListingById(Long id);
    List<ListingResponseDto> getAllListings();
    PagedResponseDto<ListingResponseDto> searchListings(ListingSearchFilterDto filter, Pageable pageable);
    ListingResponseDto updateListing(Long id, String ownerEmail, CreateListingDto request);
    void deleteListing(Long id, String ownerEmail);
    ListingResponseDto uploadListingImages(Long id, String ownerEmail, List<MultipartFile> files);
}