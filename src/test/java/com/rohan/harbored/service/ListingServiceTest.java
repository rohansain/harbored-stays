package com.rohan.harbored.service;

import com.rohan.harbored.dto.request.CreateListingDto;
import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.UnauthorizedException;
import com.rohan.harbored.mapper.ListingMapper;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.service.impl.ListingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingMapper listingMapper;

    @InjectMocks
    private ListingServiceImpl listingService;

    @Test
    @DisplayName("Should create listing successfully when host is VERIFIED")
    void shouldCreateListingWhenHostIsVerified() {
        CreateListingDto request = CreateListingDto.builder()
                .title("2BHK Flat near Campus")
                .description("Spacious furnished 2BHK flat.")
                .location("Outer Ring Road")
                .city("Bengaluru")
                .pricePerMonth(new BigDecimal("22000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .build();

        User verifiedHost = User.builder()
                .id(1L)
                .email("host@harbored.com")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        Listing entity = Listing.builder().id(100L).title("2BHK Flat near Campus").owner(verifiedHost).build();
        ListingResponseDto responseDto = ListingResponseDto.builder().id(100L).title("2BHK Flat near Campus").build();

        when(userRepository.findByEmail("host@harbored.com")).thenReturn(Optional.of(verifiedHost));
        when(listingMapper.toEntity(request, verifiedHost)).thenReturn(entity);
        when(listingRepository.save(entity)).thenReturn(entity);
        when(listingMapper.toResponseDto(entity)).thenReturn(responseDto);

        ListingResponseDto result = listingService.createListing("host@harbored.com", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        verify(listingRepository).save(entity);
    }

    @Test
    @DisplayName("Should throw BadRequestException when host is UNVERIFIED")
    void shouldRejectListingCreationWhenHostUnverified() {
        CreateListingDto request = CreateListingDto.builder()
                .title("Unverified Flat")
                .minStayMonths(1)
                .maxStayMonths(6)
                .build();

        User unverifiedHost = User.builder()
                .id(2L)
                .email("unverified@harbored.com")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .build();

        when(userRepository.findByEmail("unverified@harbored.com")).thenReturn(Optional.of(unverifiedHost));

        assertThatThrownBy(() -> listingService.createListing("unverified@harbored.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only verified users can create listings");

        verify(listingRepository, never()).save(any(Listing.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when another user tries to delete a listing")
    void shouldRejectDeletionFromNonOwner() {
        User actualOwner = User.builder().id(1L).email("owner@harbored.com").role(Role.HOST).build();
        User attacker = User.builder().id(2L).email("intruder@harbored.com").role(Role.HOST).build();

        Listing listing = Listing.builder().id(50L).owner(actualOwner).build();

        when(userRepository.findByEmail("intruder@harbored.com")).thenReturn(Optional.of(attacker));
        when(listingRepository.findById(50L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.deleteListing(50L, "intruder@harbored.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You do not have permission");

        verify(listingRepository, never()).delete(any(Listing.class));
    }
}