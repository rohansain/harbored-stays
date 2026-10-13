package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.CreateReviewDto;
import com.rohan.harbored.dto.response.ReviewResponseDto;

import java.util.List;

public interface ReviewService {
    ReviewResponseDto addReview(Long listingId, String authorEmail, CreateReviewDto request);
    List<ReviewResponseDto> getReviewsForListing(Long listingId);
    void deleteReview(Long reviewId, String userEmail);
}