package com.rohan.harbored.specification;

import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.entity.Listing;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

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

            // 1. Dynamic City Filter: Case-insensitive exact match
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

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}