package com.rohan.harbored.service.impl;

import com.rohan.harbored.dto.request.CreateReviewDto;
import com.rohan.harbored.dto.response.ReviewResponseDto;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.Review;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.ResourceNotFoundException;
import com.rohan.harbored.exception.UnauthorizedException;
import com.rohan.harbored.mapper.ReviewMapper;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.ReviewRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponseDto addReview(Long listingId, String authorEmail, CreateReviewDto request) {
        User author = userRepository.findByEmail(authorEmail.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + authorEmail));

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (listing.getOwner().getId().equals(author.getId())) {
            throw new BadRequestException("Hosts cannot review their own property");
        }

        if (reviewRepository.existsByListingIdAndAuthorId(listingId, author.getId())) {
            throw new BadRequestException("You have already submitted a review for this listing");
        }

        Review review = Review.builder()
                .rating(request.getRating())
                .comment(request.getComment().trim())
                .listing(listing)
                .author(author)
                .build();

        Review savedReview = reviewRepository.save(review);

        recalculateAndSaveListingRating(listing);

        return reviewMapper.toResponseDto(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsForListing(Long listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ResourceNotFoundException("Listing not found with id: " + listingId);
        }

        return reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId).stream()
                .map(reviewMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        boolean isAuthor = review.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new UnauthorizedException("You do not have permission to delete this review");
        }

        Listing listing = review.getListing();
        reviewRepository.delete(review);

        recalculateAndSaveListingRating(listing);
    }

    private void recalculateAndSaveListingRating(Listing listing) {
        Double updatedAverage = reviewRepository.calculateAverageRatingForListing(listing.getId());
        listing.setAverageRating(Math.round(updatedAverage * 100.0) / 100.0);
        listingRepository.save(listing);
    }
}