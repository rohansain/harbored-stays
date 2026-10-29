package com.rohan.harbored.seeder;

import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class ListingDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ListingDataSeeder.class);

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        long currentListingCount = listingRepository.count();
        if (currentListingCount >= 50) {
            log.info("Listing dataset already seeded (Total count: {}). Skipping seeder.", currentListingCount);
            return;
        }

        log.info("Current listing count ({}) is under 50. Seeding demo dataset for Day 32...", currentListingCount);

        User seedHost = userRepository.findByEmail("seeder.host@harbored.com").orElseGet(() -> {
            User host = User.builder()
                    .name("Seeded Verified Host")
                    .email("seeder.host@harbored.com")
                    .passwordHash("$2a$10$wK1RkRj4q49qKz7Kj0/E8.UaL3pQ9Rz4Z5mI6A0B1C2D3E4F5G6H7") // dummy bcrypt
                    .role(Role.HOST)
                    .verificationStatus(VerificationStatus.VERIFIED)
                    .build();
            return userRepository.save(host);
        });

        String[] cities = {"Bengaluru", "Gurugram", "Noida", "Pune", "Hyderabad"};
        String[] locations = {
                "Near Manyata Tech Park", "Cyber City Phase 2", "Sector 62 IT Hub",
                "Hinjewadi Phase 1", "Hitec City", "Koramangala 5th Block",
                "Golf Course Road", "Sector 144", "Kalyani Nagar", "Gachibowli"
        };
        String[] titleTemplates = {
                "Furnished Studio with High-Speed Wi-Fi",
                "Modern 1BHK Apartment near Metro",
                "Cozy Private Room for Working Professionals",
                "Spacious Flat with Balcony and Dedicated Workstation",
                "Quiet 1RK Studio with Power Backup"
        };
        String[] descriptions = {
                "Equipped with dedicated workstation, fiber Wi-Fi, air conditioning, and 24/7 power backup.",
                "Walking distance to key corporate offices, shared modern kitchen, microwave, and daily housekeeping.",
                "Quiet residential layout ideal for corporate trainees and interns. Includes washing machine and refrigerator.",
                "Sunny studio apartment featuring ergonomic work chair, high-speed internet, and biometric entry."
        };

        List<Listing> seedListings = new ArrayList<>();
        int startingPrice = 12000;

        for (int i = 1; i <= 52; i++) {
            String city = cities[(i - 1) % cities.length];
            String location = locations[(i - 1) % locations.length];
            String title = titleTemplates[(i - 1) % titleTemplates.length] + " #" + i;
            String description = descriptions[(i - 1) % descriptions.length];
            BigDecimal price = BigDecimal.valueOf(startingPrice + ((i * 650) % 25000)).setScale(2);
            int minStay = 1 + (i % 2); // 1 or 2 months
            int maxStay = 6 + (i % 6); // 6 to 11 months

            Listing listing = Listing.builder()
                    .title(title)
                    .description(description)
                    .location(location)
                    .city(city)
                    .pricePerMonth(price)
                    .minStayMonths(minStay)
                    .maxStayMonths(maxStay)
                    .owner(seedHost)
                    .averageRating(Math.round((4.0 + (i % 10) * 0.1) * 100.0) / 100.0)
                    .build();

            seedListings.add(listing);
        }

        listingRepository.saveAll(seedListings);
        log.info("Successfully seeded {} realistic listings into the database.", seedListings.size());
    }
}