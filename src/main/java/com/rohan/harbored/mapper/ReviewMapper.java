package com.rohan.harbored.mapper;

import com.rohan.harbored.dto.response.ReviewResponseDto;
import com.rohan.harbored.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponseDto toResponseDto(Review review) {
        if (review == null) {
            return null;
        }

        return ReviewResponseDto.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .listingId(review.getListing() != null ? review.getListing().getId() : null)
                .authorId(review.getAuthor() != null ? review.getAuthor().getId() : null)
                .authorName(review.getAuthor() != null ? review.getAuthor().getName() : null)
                .createdAt(review.getCreatedAt())
                .build();
    }
}