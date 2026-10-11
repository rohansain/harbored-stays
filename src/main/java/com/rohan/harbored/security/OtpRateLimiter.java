package com.rohan.harbored.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OtpRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 900; // 15 minutes window

    private final ConcurrentHashMap<String, RateLimitTracker> attemptStore = new ConcurrentHashMap<>();

    public boolean isAllowed(String email) {
        Instant now = Instant.now();
        RateLimitTracker tracker = attemptStore.compute(email.toLowerCase().trim(), (key, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return new RateLimitTracker(now);
            }
            existing.increment();
            return existing;
        });

        return tracker.getAttempts() <= MAX_ATTEMPTS;
    }

    public void clear(String email) {
        attemptStore.remove(email.toLowerCase().trim());
    }

    private static class RateLimitTracker {
        private final Instant windowStart;
        private final AtomicInteger attempts;

        public RateLimitTracker(Instant windowStart) {
            this.windowStart = windowStart;
            this.attempts = new AtomicInteger(1);
        }

        public void increment() {
            this.attempts.incrementAndGet();
        }

        public int getAttempts() {
            return this.attempts.get();
        }

        public boolean isExpired(Instant now) {
            return now.isAfter(windowStart.plusSeconds(WINDOW_SECONDS));
        }
    }
}