package com.rohan.harbored.specification;

import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.enums.BookingStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ListingSpecification {

    private ListingSpecification() {
        // Prevent direct instantiation of utility class
    }

    public static Specification<Listing> buildSearchSpecification(ListingSearchFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // 1. Dynamic City Filter: Case-insensitive match
            if (filter.getCity() != null && !filter.getCity().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("city")),
                        filter.getCity().trim().toLowerCase()
                ));
            }

            // 2. Dynamic Minimum Price Filter: pricePerMonth >= minPrice
            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("pricePerMonth"),
                        filter.getMinPrice()
                ));
            }

            // 3. Dynamic Maximum Price Filter: pricePerMonth <= maxPrice
            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("pricePerMonth"),
                        filter.getMaxPrice()
                ));
            }

            // 4. Date-Range Availability: Exclude listings with overlapping CONFIRMED bookings
            LocalDate requestedStart = filter.getStartDate();
            LocalDate requestedEnd = filter.getEndDate();

            if (requestedStart != null && requestedEnd != null) {
                // Subquery to find any conflicting CONFIRMED booking for this listing
                Subquery<Long> bookingSubquery = query.subquery(Long.class);
                Root<Booking> bookingRoot = bookingSubquery.from(Booking.class);

                Predicate listingCorrelation = criteriaBuilder.equal(bookingRoot.get("listing"), root);
                Predicate statusConfirmed = criteriaBuilder.equal(bookingRoot.get("status"), BookingStatus.CONFIRMED);
                Predicate startBeforeRequestedEnd = criteriaBuilder.lessThan(bookingRoot.get("startDate"), requestedEnd);
                Predicate endAfterRequestedStart = criteriaBuilder.greaterThan(bookingRoot.get("endDate"), requestedStart);

                bookingSubquery.select(bookingRoot.get("id"))
                        .where(criteriaBuilder.and(
                                listingCorrelation,
                                statusConfirmed,
                                startBeforeRequestedEnd,
                                endAfterRequestedStart
                        ));

                // NOT EXISTS (conflicting booking)
                predicates.add(criteriaBuilder.not(criteriaBuilder.exists(bookingSubquery)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}