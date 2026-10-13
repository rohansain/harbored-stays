package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.CreateReviewDto;
import com.rohan.harbored.dto.response.ReviewResponseDto;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.Review;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.UnauthorizedException;
import com.rohan.harbored.mapper.ReviewMapper;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.ReviewRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    @DisplayName("Should successfully add a review and update listing average rating")
    void shouldAddReviewSuccessfully() {
        User host = User.builder().id(1L).email("host@harbored.com").build();
        User guest = User.builder().id(2L).email("guest@harbored.com").build();
        Listing listing = Listing.builder().id(10L).owner(host).averageRating(0.0).build();

        CreateReviewDto request = CreateReviewDto.builder()
                .rating(5)
                .comment("Excellent place for internship stay!")
                .build();

        Review savedReview = Review.builder().id(100L).rating(5).comment("Excellent place!").listing(listing).author(guest).build();

        when(userRepository.findByEmail("guest@harbored.com")).thenReturn(Optional.of(guest));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(reviewRepository.existsByListingIdAndAuthorId(10L, 2L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewRepository.calculateAverageRatingForListing(10L)).thenReturn(5.0);
        when(reviewMapper.toResponseDto(savedReview)).thenReturn(ReviewResponseDto.builder().id(100L).rating(5).build());

        ReviewResponseDto result = reviewService.addReview(10L, "guest@harbored.com", request);

        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(listing.getAverageRating()).isEqualTo(5.0);
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("Should reject review submission when host attempts to review own listing")
    void shouldRejectHostSelfReview() {
        User host = User.builder().id(1L).email("host@harbored.com").build();
        Listing listing = Listing.builder().id(10L).owner(host).build();

        CreateReviewDto request = CreateReviewDto.builder().rating(5).comment("Self promotion").build();

        when(userRepository.findByEmail("host@harbored.com")).thenReturn(Optional.of(host));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> reviewService.addReview(10L, "host@harbored.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Hosts cannot review their own property");

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Should reject review deletion by non-author user")
    void shouldRejectReviewDeletionByUnauthorizedUser() {
        User author = User.builder().id(2L).email("author@harbored.com").role(Role.GUEST).build();
        User intruder = User.builder().id(3L).email("intruder@harbored.com").role(Role.GUEST).build();
        Review review = Review.builder().id(50L).author(author).build();

        when(userRepository.findByEmail("intruder@harbored.com")).thenReturn(Optional.of(intruder));
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(50L, "intruder@harbored.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You do not have permission");

        verify(reviewRepository, never()).delete(any(Review.class));
    }
}