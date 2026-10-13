package com.rohan.harbored.repository;

import com.rohan.harbored.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByListingIdOrderByCreatedAtDesc(Long listingId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.listing.id = :listingId")
    Double calculateAverageRatingForListing(@Param("listingId") Long listingId);

    boolean existsByListingIdAndAuthorId(Long listingId, Long authorId);
}