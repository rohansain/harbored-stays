package com.rohan.harbored.service;

import com.rohan.harbored.dto.response.ListingResponseDto;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingImageServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingMapper listingMapper;

    @Mock
    private DocumentStorageService documentStorageService;

    @InjectMocks
    private ListingServiceImpl listingService;

    @Test
    @DisplayName("Should successfully upload images and append to listing when caller is owner")
    void shouldUploadListingImagesSuccessfully() {
        User host = User.builder().id(1L).email("host@harbored.com").role(Role.HOST).build();
        Listing listing = Listing.builder().id(20L).owner(host).images(new ArrayList<>()).build();

        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(file1, file2);
        List<String> uploadedUrls = List.of(
                "https://res.cloudinary.com/demo/image/upload/v1/living.jpg",
                "https://res.cloudinary.com/demo/image/upload/v1/bedroom.jpg"
        );

        when(userRepository.findByEmail("host@harbored.com")).thenReturn(Optional.of(host));
        when(listingRepository.findById(20L)).thenReturn(Optional.of(listing));
        when(documentStorageService.uploadListingImages(files, 20L)).thenReturn(uploadedUrls);
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);
        when(listingMapper.toResponseDto(listing)).thenReturn(ListingResponseDto.builder()
                .id(20L)
                .images(uploadedUrls)
                .build());

        ListingResponseDto response = listingService.uploadListingImages(20L, "host@harbored.com", files);

        assertThat(response).isNotNull();
        assertThat(response.getImages()).hasSize(2);
        assertThat(listing.getImages()).containsAll(uploadedUrls);
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("Should reject upload when user is not owner and not admin")
    void shouldRejectUploadWhenNotOwner() {
        User actualOwner = User.builder().id(1L).email("owner@harbored.com").role(Role.HOST).build();
        User otherHost = User.builder().id(2L).email("other@harbored.com").role(Role.HOST).build();
        Listing listing = Listing.builder().id(20L).owner(actualOwner).build();

        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(file);

        when(userRepository.findByEmail("other@harbored.com")).thenReturn(Optional.of(otherHost));
        when(listingRepository.findById(20L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.uploadListingImages(20L, "other@harbored.com", files))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You do not have permission");

        verify(documentStorageService, never()).uploadListingImages(any(), any());
        verify(listingRepository, never()).save(any());
    }
}