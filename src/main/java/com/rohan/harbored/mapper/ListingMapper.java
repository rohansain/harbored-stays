package com.rohan.harbored.mapper;

import com.rohan.harbored.dto.request.CreateListingDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ListingMapper {

    public Listing toEntity(CreateListingDto dto, User owner) {
        if (dto == null) {
            return null;
        }

        return Listing.builder()
                .title(dto.getTitle().trim())
                .description(dto.getDescription().trim())
                .location(dto.getLocation().trim())
                .city(dto.getCity().trim())
                .pricePerMonth(dto.getPricePerMonth())
                .minStayMonths(dto.getMinStayMonths())
                .maxStayMonths(dto.getMaxStayMonths())
                .images(dto.getImages() != null ? new ArrayList<>(dto.getImages()) : new ArrayList<>())
                .owner(owner)
                .averageRating(0.0)
                .build();
    }

    public ListingResponseDto toResponseDto(Listing listing) {
        if (listing == null) {
            return null;
        }

        return ListingResponseDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .location(listing.getLocation())
                .city(listing.getCity())
                .pricePerMonth(listing.getPricePerMonth())
                .minStayMonths(listing.getMinStayMonths())
                .maxStayMonths(listing.getMaxStayMonths())
                .images(listing.getImages())
                .averageRating(listing.getAverageRating())
                .ownerId(listing.getOwner() != null ? listing.getOwner().getId() : null)
                .ownerName(listing.getOwner() != null ? listing.getOwner().getName() : null)
                .ownerEmail(listing.getOwner() != null ? listing.getOwner().getEmail() : null)
                .createdAt(listing.getCreatedAt())
                .build();
    }
}