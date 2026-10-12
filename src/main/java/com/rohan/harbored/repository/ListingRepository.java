package com.rohan.harbored.repository;

import com.rohan.harbored.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByCityIgnoreCase(String city);

    List<Listing> findByOwnerId(Long ownerId);
}