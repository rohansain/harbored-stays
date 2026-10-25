package com.rohan.harbored.repository;

import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByGuestIdOrderByCreatedAtDesc(Long guestId);

    List<Booking> findByListingIdOrderByStartDateAsc(Long listingId);

    List<Booking> findByListingIdAndStatus(Long listingId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.listing.owner.id = :hostId AND b.status = :status ORDER BY b.createdAt DESC")
    List<Booking> findByHostIdAndStatus(@Param("hostId") Long hostId, @Param("status") BookingStatus status);

    List<Booking> findByStatusAndEndDateBefore(BookingStatus status, LocalDate date);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
            "WHERE b.listing.id = :listingId " +
            "AND b.status = :status " +
            "AND b.startDate < :requestedEndDate " +
            "AND b.endDate > :requestedStartDate")
    boolean existsOverlappingBooking(
            @Param("listingId") Long listingId,
            @Param("status") BookingStatus status,
            @Param("requestedStartDate") LocalDate requestedStartDate,
            @Param("requestedEndDate") LocalDate requestedEndDate
    );

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
            "WHERE b.listing.id = :listingId " +
            "AND b.guest.id = :guestId " +
            "AND b.status = :status " +
            "AND b.startDate < :requestedEndDate " +
            "AND b.endDate > :requestedStartDate")
    boolean existsOverlappingBookingForGuest(
            @Param("listingId") Long listingId,
            @Param("guestId") Long guestId,
            @Param("status") BookingStatus status,
            @Param("requestedStartDate") LocalDate requestedStartDate,
            @Param("requestedEndDate") LocalDate requestedEndDate
    );
}