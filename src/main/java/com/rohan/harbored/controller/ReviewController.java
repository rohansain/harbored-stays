package com.rohan.harbored.controller;

import com.rohan.harbored.dto.request.CreateReviewDto;
import com.rohan.harbored.dto.response.ApiResponse;
import com.rohan.harbored.dto.response.ReviewResponseDto;
import com.rohan.harbored.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/listings/{listingId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> addReview(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateReviewDto request
    ) {
        ReviewResponseDto createdReview = reviewService.addReview(listingId, userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdReview, "Review submitted successfully"));
    }

    @GetMapping("/listings/{listingId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponseDto>>> getReviewsForListing(@PathVariable Long listingId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsForListing(listingId);
        return ResponseEntity.ok(ApiResponse.success(reviews, "Reviews retrieved successfully"));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        reviewService.deleteReview(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted successfully"));
    }
}