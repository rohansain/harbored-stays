package com.rohan.harbored.service;

import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class DatabaseSetupTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void cleanDatabaseAndCreateUsers() {

        // Delete existing users
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        User rahul = User.builder()
                .name("Rahul")
                .email("rahul@gmail.com")
                .passwordHash(passwordEncoder.encode("rahul@123"))
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        User tiwari = User.builder()
                .name("Tiwari")
                .email("tiwari@gmail.com")
                .passwordHash(passwordEncoder.encode("tiwari@123"))
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        User vedant = User.builder()
                .name("Vedant")
                .email("vedant@gmail.com")
                .passwordHash(passwordEncoder.encode("vedant@123"))
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        User rohan = User.builder()
                .name("Rohan")
                .email("rohan@gmail.com")
                .passwordHash(passwordEncoder.encode("rohan@123"))
                .role(Role.ADMIN)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        userRepository.saveAll(
                java.util.List.of(rahul, tiwari, vedant, rohan)
        );
    }
}