package com.rohan.harbored.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenBucketTest {

    @Test
    @DisplayName("Should allow bursts up to capacity and reject when tokens are depleted")
    void shouldAllowBurstsUpToCapacityAndRejectAfter() {
        // Bucket with capacity 3, replenishing 1 token every minute
        TokenBucket bucket = new TokenBucket(3, 1, TimeUnit.MINUTES);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();

        // 4th request must be rejected immediately
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    @DisplayName("Should lazily replenish tokens after duration elapses")
    void shouldReplenishTokensAfterDuration() throws InterruptedException {
        // Bucket with capacity 2, replenishing 10 tokens per second (1 token every 100ms)
        TokenBucket bucket = new TokenBucket(2, 10, TimeUnit.SECONDS);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();

        // Wait 150 milliseconds to allow ~1.5 tokens to replenish
        Thread.sleep(150);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    @DisplayName("Tokens should not exceed maximum capacity during prolonged idle periods")
    void shouldNotExceedCapacityOnProlongedIdle() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(2, 100, TimeUnit.SECONDS);

        // Wait 100ms
        Thread.sleep(100);

        // Tokens should still be capped at 2
        assertThat(bucket.getAvailableTokens()).isLessThanOrEqualTo(2.0);
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    @DisplayName("Should reject invalid configuration arguments")
    void shouldRejectInvalidArguments() {
        assertThatThrownBy(() -> new TokenBucket(0, 10, TimeUnit.SECONDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacity must be strictly greater than 0");

        assertThatThrownBy(() -> new TokenBucket(10, 0, TimeUnit.SECONDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refill tokens must be strictly greater than 0");
    }

    @Test
    @DisplayName("Should ensure thread-safety under concurrent consumption")
    void shouldBeThreadSafeUnderConcurrentLoad() throws InterruptedException {
        int capacity = 50;
        int totalThreads = 100;
        TokenBucket bucket = new TokenBucket(capacity, 1, TimeUnit.HOURS); // effectively no refill during test

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (bucket.tryConsume()) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Exactly 50 threads must succeed and 50 must be rejected
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(rejectedCount.get()).isEqualTo(50);
        assertThat(bucket.tryConsume()).isFalse();
    }
}