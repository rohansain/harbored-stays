package com.rohan.harbored.controller;

import com.rohan.harbored.dto.request.CreateListingDto;
import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.dto.response.ApiResponse;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.dto.response.PagedResponseDto;
import com.rohan.harbored.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

    @PostMapping
    @PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListingResponseDto>> createListing(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateListingDto request
    ) {
        ListingResponseDto response = listingService.createListing(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Listing created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ListingResponseDto>>> getAllListings() {
        List<ListingResponseDto> responses = listingService.getAllListings();
        return ResponseEntity.ok(ApiResponse.success(responses, "Listings retrieved successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponseDto<ListingResponseDto>>> searchListings(
            @Valid @ModelAttribute ListingSearchFilterDto filter,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponseDto<ListingResponseDto> response = listingService.searchListings(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Search results retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponseDto>> getListingById(@PathVariable Long id) {
        ListingResponseDto response = listingService.getListingById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Listing retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListingResponseDto>> updateListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateListingDto request
    ) {
        ListingResponseDto response = listingService.updateListing(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Listing updated successfully"));
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
    public ResponseEntity<ApiResponse<ListingResponseDto>> uploadImages(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("files") List<MultipartFile> files
    ) {
        ListingResponseDto response = listingService.uploadListingImages(id, userDetails.getUsername(), files);
        return ResponseEntity.ok(ApiResponse.success(response, "Listing images uploaded successfully"));
    }
}