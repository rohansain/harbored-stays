package com.rohan.harbored.service.impl;

import com.rohan.harbored.dto.request.CreateListingDto;
import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import com.rohan.harbored.dto.request.UpdateListingDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.dto.response.PagedResponseDto;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.ResourceNotFoundException;
import com.rohan.harbored.exception.UnauthorizedException;
import com.rohan.harbored.mapper.ListingMapper;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.DocumentStorageService;
import com.rohan.harbored.service.ListingService;
import com.rohan.harbored.specification.ListingSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ListingMapper listingMapper;
    private final DocumentStorageService documentStorageService;

    @Override
    @Transactional
    public ListingResponseDto createListing(String userEmail, CreateListingDto request) {
        User user = getUserByEmail(userEmail);

        if (user.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BadRequestException("Only verified users can create listings. Current status: " + user.getVerificationStatus());
        }

        if (request.getMinStayMonths() > request.getMaxStayMonths()) {
            throw new BadRequestException("Minimum stay months cannot exceed maximum stay months");
        }

        Listing listing = listingMapper.toEntity(request, user);
        Listing savedListing = listingRepository.save(listing);
        return listingMapper.toResponseDto(savedListing);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingResponseDto getListingById(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        return listingMapper.toResponseDto(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponseDto> getAllListings() {
        return listingRepository.findAll().stream()
                .map(listingMapper::toResponseDto)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<ListingResponseDto> searchListings(ListingSearchFilterDto filter, Pageable pageable) {
        if (filter != null) {
            if (filter.getMinPrice() != null && filter.getMaxPrice() != null) {
                if (filter.getMinPrice().compareTo(filter.getMaxPrice()) > 0) {
                    throw new BadRequestException("minPrice cannot be greater than maxPrice");
                }
            }

            LocalDate start = filter.getStartDate();
            LocalDate end = filter.getEndDate();

            if ((start != null && end == null) || (start == null && end != null)) {
                throw new BadRequestException("Both startDate and endDate must be provided together for date-range filtering");
            }

            if (start != null && end != null) {
                if (!start.isBefore(end)) {
                    throw new BadRequestException("startDate must be strictly before endDate");
                }
                if (start.isBefore(LocalDate.now())) {
                    throw new BadRequestException("startDate cannot be in the past");
                }
            }
        }

        Specification<Listing> spec = ListingSpecification.buildSearchSpecification(filter);
        Page<Listing> listingPage = listingRepository.findAll(spec, pageable);

        Page<ListingResponseDto> dtoPage = listingPage.map(listingMapper::toResponseDto);
        return PagedResponseDto.of(dtoPage);
    }

    @Override
    @Transactional
    public ListingResponseDto updateListing(Long id, String ownerEmail, CreateListingDto request) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + id));

        User caller = getUserByEmail(ownerEmail);
        validateOwnership(listing, caller);

        if (request.getMinStayMonths() > request.getMaxStayMonths()) {
            throw new BadRequestException("Minimum stay months cannot exceed maximum stay months");
        }

        listing.setTitle(request.getTitle().trim());
        listing.setDescription(request.getDescription().trim());
        listing.setLocation(request.getLocation().trim());
        listing.setCity(request.getCity().trim());
        listing.setPricePerMonth(request.getPricePerMonth());
        listing.setMinStayMonths(request.getMinStayMonths());
        listing.setMaxStayMonths(request.getMaxStayMonths());
        Listing updated = listingRepository.save(listing);
        return listingMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteListing(Long listingId, String userEmail) {
        User currentUser = getUserByEmail(userEmail);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        validateOwnershipOrAdmin(listing, currentUser);

        listingRepository.delete(listing);
    }

    @Override
    @Transactional
    public ListingResponseDto uploadListingImages(Long listingId, String userEmail, List<MultipartFile> files) {
        User currentUser = getUserByEmail(userEmail);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        validateOwnershipOrAdmin(listing, currentUser);

        List<String> uploadedUrls = documentStorageService.uploadListingImages(files, listing.getId());

        List<String> existingImages = listing.getImages();
        if (existingImages == null) {
            existingImages = new ArrayList<>();
        }
        existingImages.addAll(uploadedUrls);
        listing.setImages(existingImages);

        Listing savedListing = listingRepository.save(listing);
        return listingMapper.toResponseDto(savedListing);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private void validateOwnershipOrAdmin(Listing listing, User currentUser) {
        boolean isOwner = listing.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You do not have permission to modify or delete this listing");
        }
    }

    private void validateOwnership(Listing listing, User caller) {
        boolean isOwner = listing.getOwner().getId().equals(caller.getId());
        boolean isAdmin = caller.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You do not have permission to modify or delete this listing");
        }
    }
}