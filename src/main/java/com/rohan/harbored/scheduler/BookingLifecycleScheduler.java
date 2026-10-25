package com.rohan.harbored.scheduler;

import com.rohan.harbored.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingLifecycleScheduler.class);

    private final BookingService bookingService;

    // Executes automatically at midnight (00:00:00) every day
    @Scheduled(cron = "0 0 0 * * *")
    public void autoMarkCompletedBookings() {
        log.info("Running daily scheduled task: autoMarkCompletedBookings");
        int updatedCount = bookingService.markExpiredBookingsAsCompleted();
        log.info("Daily scheduled task finished. Completed {} bookings.", updatedCount);
    }
}