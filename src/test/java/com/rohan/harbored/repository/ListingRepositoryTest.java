package com.rohan.harbored.repository;

import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListingRepositoryTest {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should persist a listing with an associated owner and fetch by city successfully")
    void shouldSaveAndFetchListingByCity() {
        User host = User.builder()
                .name("Super Host")
                .email("host.verified@harbored.com")
                .passwordHash("hashed_secret_code")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        User savedHost = userRepository.save(host);

        Listing listing = Listing.builder()
                .title("Cozy Studio near Tech Park")
                .description("Fully furnished studio with high-speed internet for interns.")
                .location("Whitefield")
                .city("Bengaluru")
                .pricePerMonth(new BigDecimal("18500.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .images(List.of("https://res.cloudinary.com/test/image1.jpg"))
                .owner(savedHost)
                .build();

        Listing savedListing = listingRepository.save(listing);

        assertThat(savedListing.getId()).isNotNull();
        assertThat(savedListing.getCreatedAt()).isNotNull();

        List<Listing> listingsInCity = listingRepository.findByCityIgnoreCase("bengaluru");
        assertThat(listingsInCity).isNotEmpty();
        assertThat(listingsInCity.get(0).getTitle()).isEqualTo("Cozy Studio near Tech Park");
        assertThat(listingsInCity.get(0).getOwner().getEmail()).isEqualTo("host.verified@harbored.com");
    }
}