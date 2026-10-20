package com.rohan.harbored.concurrency;

import com.rohan.harbored.entity.Booking;
import com.rohan.harbored.entity.Listing;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.BookingStatus;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.repository.BookingRepository;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingOptimisticLockingTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    private Long testBookingId;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        User host = User.builder()
                .name("Kavya Host")
                .email("kavya.host@harbored.com")
                .passwordHash("encoded_bcrypt")
                .role(Role.HOST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(host);

        User guest = User.builder()
                .name("Arjun Guest")
                .email("arjun.guest@harbored.com")
                .passwordHash("encoded_bcrypt")
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        userRepository.save(guest);

        Listing listing = Listing.builder()
                .title("Furnished 1BHK near Cyber City")
                .description("Includes high-speed Wi-Fi and power backup.")
                .location("DLF Phase 2")
                .city("Gurugram")
                .pricePerMonth(new BigDecimal("20000.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .owner(host)
                .build();
        listingRepository.save(listing);

        Booking booking = Booking.builder()
                .listing(listing)
                .guest(guest)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(32))
                .totalPrice(new BigDecimal("20000.00"))
                .status(BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.saveAndFlush(booking);
        testBookingId = saved.getId();
    }

    @Test
    @DisplayName("Should allow only one thread to mutate status and fail the other with Optimistic Locking collision")
    void shouldHandleConcurrentModificationsWithOptimisticLocking() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    // Both threads wait here until released simultaneously
                    startLatch.await();

                    // Read entity snapshot concurrently (both see version = 0)
                    Booking bookingSnapshot = bookingRepository.findById(testBookingId).orElseThrow();

                    // Introduce a tiny delay on the second thread to simulate race conditions
                    if (threadIndex == 1) {
                        Thread.sleep(50);
                    }

                    // Attempt mutation
                    bookingSnapshot.setStatus(threadIndex == 0 ? BookingStatus.CONFIRMED : BookingStatus.CANCELLED);
                    bookingRepository.saveAndFlush(bookingSnapshot);

                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // Ignored for other unhandled thread interruptions
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire both threads simultaneously
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Exactly one thread succeeded in bumping version 0 -> 1
        assertThat(successCount.get()).isEqualTo(1);
        // Exactly one thread collided and threw ObjectOptimisticLockingFailureException
        assertThat(conflictCount.get()).isEqualTo(1);

        Booking finalBooking = bookingRepository.findById(testBookingId).orElseThrow();
        assertThat(finalBooking.getVersion()).isEqualTo(1L);
    }
}