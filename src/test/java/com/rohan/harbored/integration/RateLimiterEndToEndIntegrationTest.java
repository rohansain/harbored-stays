package com.rohan.harbored.integration;

import com.rohan.harbored.security.ratelimit.RateLimitProperties;
import com.rohan.harbored.security.ratelimit.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimiterEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        rateLimiterService.clearRegistry();
    }

    @Test
    @DisplayName("Should enforce HTTP 429 when anonymous capacity is exhausted via rapid requests")
    void shouldReturn429WhenAnonymousCapacityExhausted() throws Exception {
        properties.getAnonymous().setCapacity(3);
        properties.getAnonymous().setRefillTokensPerMinute(1);

        String clientIp = "192.0.2.45";

        // First 3 requests must succeed
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/listings/search")
                            .with(anonymous())
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        // 4th request must immediately fail with HTTP 429
        mockMvc.perform(get("/api/v1/listings/search")
                        .with(anonymous())
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value("Too many requests. Please slow down and try again shortly."));
    }

    @Test
    @DisplayName("Should replenish tokens after refill duration and allow subsequent requests")
    void shouldReplenishTokensAfterDuration() throws Exception {
        properties.getAnonymous().setCapacity(2);
        properties.getAnonymous().setRefillTokensPerMinute(120); // 2 tokens/sec = 1 token per 500ms

        String clientIp = "192.0.2.99";

        // Consume both tokens
        mockMvc.perform(get("/api/v1/listings/search")
                        .with(anonymous())
                        .header("X-Forwarded-For", clientIp))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/listings/search")
                        .with(anonymous())
                        .header("X-Forwarded-For", clientIp))
                .andExpect(status().isOk());

        // 3rd immediate request must be blocked
        mockMvc.perform(get("/api/v1/listings/search")
                        .with(anonymous())
                        .header("X-Forwarded-For", clientIp))
                .andExpect(status().isTooManyRequests());

        // Sleep 600ms to allow 1.2 tokens to lazily replenish
        Thread.sleep(600);

        // Subsequent request must now succeed
        mockMvc.perform(get("/api/v1/listings/search")
                        .with(anonymous())
                        .header("X-Forwarded-For", clientIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Dual-Tier Isolation: Depleting anonymous IP bucket must not throttle authenticated user")
    void shouldIsolateAuthenticatedUserFromExhaustedIpBucket() throws Exception {
        // Strict anonymous limit: 2 requests
        properties.getAnonymous().setCapacity(2);
        properties.getAnonymous().setRefillTokensPerMinute(1);

        // Authenticated limit: 10 requests
        properties.getAuthenticated().setCapacity(10);
        properties.getAuthenticated().setRefillTokensPerMinute(60);

        String sharedIp = "198.51.100.12";

        // 1. Two anonymous calls exhaust the IP bucket
        mockMvc.perform(get("/api/v1/listings/search")
                        .with(anonymous())
                        .header("X-Forwarded-For", sharedIp))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/listings/search")
                        .with(anonymous())
                        .header("X-Forwarded-For", sharedIp))
                .andExpect(status().isOk());

        // 2. 3rd anonymous request from this IP is blocked with 429
        mockMvc.perform(get("/api/v1/listings/search")
                        .with(anonymous())
                        .header("X-Forwarded-For", sharedIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));

        // 3. An authenticated user from the SAME IP must succeed (evaluated against user:rohan.backend@harbored.com)
        mockMvc.perform(get("/api/v1/listings/search")
                        .with(user("rohan.backend@harbored.com").roles("GUEST"))
                        .header("X-Forwarded-For", sharedIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}