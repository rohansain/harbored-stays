package com.rohan.harbored.security.ratelimit;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucket {

    private final long capacity;
    private final double refillTokensPerNano;
    private final ReentrantLock lock = new ReentrantLock();

    private double availableTokens;
    private long lastRefillTimestampNanos;

    /**
     * Constructs a TokenBucket.
     *
     * @param capacity          maximum burst capacity (maximum tokens the bucket can hold)
     * @param refillTokens      number of tokens added to the bucket over the specified duration
     * @param refillDuration    duration unit for token replenishment
     */
    public TokenBucket(long capacity, long refillTokens, TimeUnit refillDuration) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Bucket capacity must be strictly greater than 0");
        }
        if (refillTokens <= 0) {
            throw new IllegalArgumentException("Refill tokens must be strictly greater than 0");
        }

        this.capacity = capacity;
        this.availableTokens = capacity;
        this.lastRefillTimestampNanos = System.nanoTime();

        long durationNanos = refillDuration.toNanos(1);
        this.refillTokensPerNano = (double) refillTokens / durationNanos;
    }

    /**
     * Attempts to consume 1 token.
     *
     * @return true if token was available and consumed; false if rate limit is exceeded
     */
    public boolean tryConsume() {
        return tryConsume(1);
    }

    /**
     * Attempts to consume a specified number of tokens.
     *
     * @param tokensToConsume number of tokens requested
     * @return true if tokens were available and consumed; false otherwise
     */
    public boolean tryConsume(long tokensToConsume) {
        if (tokensToConsume <= 0) {
            throw new IllegalArgumentException("Tokens to consume must be strictly greater than 0");
        }

        lock.lock();
        try {
            refill();

            if (availableTokens >= tokensToConsume) {
                availableTokens -= tokensToConsume;
                return true;
            }

            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Lazily adds accumulated tokens based on elapsed nanoseconds since last check.
     */
    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillTimestampNanos;

        if (elapsedNanos > 0) {
            double tokensToAdd = elapsedNanos * refillTokensPerNano;
            this.availableTokens = Math.min((double) capacity, availableTokens + tokensToAdd);
            this.lastRefillTimestampNanos = now;
        }
    }

    public long getCapacity() {
        return capacity;
    }

    public double getAvailableTokens() {
        lock.lock();
        try {
            refill();
            return availableTokens;
        } finally {
            lock.unlock();
        }
    }
}