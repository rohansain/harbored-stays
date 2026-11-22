package com.rohan.harbored.integration;

import com.rohan.harbored.dto.response.ListingResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisConnectionSmokeTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Should write, read, and delete serialized DTO in Redis with TTL")
    void shouldReadAndWriteDtoToRedis() {
        String testKey = "harbored:smoke:listing:999";

        ListingResponseDto sampleDto = ListingResponseDto.builder()
                .id(999L)
                .title("Redis Smoke Test Studio")
                .city("Bengaluru")
                .pricePerMonth(new BigDecimal("25000.00"))
                .createdAt(Instant.now())
                .build();

        // 1. Write to Redis with 60-second TTL
        redisTemplate.opsForValue().set(testKey, sampleDto, Duration.ofSeconds(60));

        // 2. Read back from Redis and verify strong deserialization
        Object cachedValue = redisTemplate.opsForValue().get(testKey);
        assertThat(cachedValue).isInstanceOf(ListingResponseDto.class);

        ListingResponseDto retrieved = (ListingResponseDto) cachedValue;
        assertThat(retrieved.getId()).isEqualTo(999L);
        assertThat(retrieved.getTitle()).isEqualTo("Redis Smoke Test Studio");
        assertThat(retrieved.getPricePerMonth()).isEqualByComparingTo("25000.00");

        // 3. Verify TTL is active
        Long ttlSeconds = redisTemplate.getExpire(testKey);
        assertThat(ttlSeconds).isNotNull().isGreaterThan(0L).isLessThanOrEqualTo(60L);

        // 4. Clean up test key
        Boolean deleted = redisTemplate.delete(testKey);
        assertThat(deleted).isTrue();
    }
}