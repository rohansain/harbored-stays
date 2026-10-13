package com.rohan.harbored.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponseDto {

    private Long id;
    private Integer rating;
    private String comment;
    private Long listingId;
    private Long authorId;
    private String authorName;
    private Instant createdAt;
}