package com.rohan.harbored.repository;

import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByGuestIdOrderByCreatedAtDesc(Long guestId);

    List<Booking> findByListingIdOrderByStartDateAsc(Long listingId);

    List<Booking> findByListingIdAndStatus(Long listingId, BookingStatus status);
}