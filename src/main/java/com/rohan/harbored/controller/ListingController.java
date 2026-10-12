package com.rohan.harbored.controller;

import com.rohan.harbored.dto.request.CreateListingDto;
import com.rohan.harbored.dto.request.UpdateListingDto;
import com.rohan.harbored.dto.response.ApiResponse;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ListingResponseDto>>> getAllListings() {
        List<ListingResponseDto> listings = listingService.getAllListings();
        return ResponseEntity.ok(ApiResponse.success(listings, "Listings retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponseDto>> getListingById(@PathVariable Long id) {
        ListingResponseDto listing = listingService.getListingById(id);
        return ResponseEntity.ok(ApiResponse.success(listing, "Listing details retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListingResponseDto>> createListing(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateListingDto request
    ) {
        ListingResponseDto createdListing = listingService.createListing(userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdListing, "Listing created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListingResponseDto>> updateListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateListingDto request
    ) {
        ListingResponseDto updatedListing = listingService.updateListing(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(updatedListing, "Listing updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        listingService.deleteListing(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Listing deleted successfully"));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListingResponseDto>> uploadListingImages(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("files") List<MultipartFile> files
    ) {
        ListingResponseDto updatedListing = listingService.uploadListingImages(id, userDetails.getUsername(), files);
        return ResponseEntity.ok(ApiResponse.success(updatedListing, "Listing images uploaded successfully"));
    }
}