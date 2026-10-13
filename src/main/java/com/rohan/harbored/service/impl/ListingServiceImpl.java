package com.rohan.harbored.service.impl;

import com.rohan.harbored.dto.request.CreateListingDto;
import com.rohan.harbored.dto.request.UpdateListingDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    @Transactional
    public ListingResponseDto updateListing(Long listingId, String userEmail, UpdateListingDto request) {
        User currentUser = getUserByEmail(userEmail);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        validateOwnershipOrAdmin(listing, currentUser);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            listing.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            listing.setDescription(request.getDescription().trim());
        }
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            listing.setLocation(request.getLocation().trim());
        }
        if (request.getCity() != null && !request.getCity().isBlank()) {
            listing.setCity(request.getCity().trim());
        }
        if (request.getPricePerMonth() != null) {
            listing.setPricePerMonth(request.getPricePerMonth());
        }
        if (request.getMinStayMonths() != null) {
            listing.setMinStayMonths(request.getMinStayMonths());
        }
        if (request.getMaxStayMonths() != null) {
            listing.setMaxStayMonths(request.getMaxStayMonths());
        }
        if (listing.getMinStayMonths() > listing.getMaxStayMonths()) {
            throw new BadRequestException("Minimum stay months cannot exceed maximum stay months");
        }
        if (request.getImages() != null) {
            listing.setImages(new ArrayList<>(request.getImages()));
        }

        Listing updatedListing = listingRepository.save(listing);
        return listingMapper.toResponseDto(updatedListing);
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
}