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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimiterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        rateLimiterService.clearRegistry();
    }

    @Test
    @DisplayName("Anonymous client should hit HTTP 429 when anonymous capacity is exhausted")
    void shouldReturn429WhenAnonymousLimitExceeded() throws Exception {
        // Temporarily set a low capacity for testing: 3 requests
        properties.getAnonymous().setCapacity(3);
        properties.getAnonymous().setRefillTokensPerMinute(1);

        String testIp = "203.0.113.195";

        // First 3 requests should succeed
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/listings/search")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // 4th request must be rejected with 429
        mockMvc.perform(get("/api/v1/listings/search")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    @WithMockUser(username = "arjun.student@harbored.com", roles = {"GUEST"})
    @DisplayName("Authenticated user gets independent, higher rate-limit bucket")
    void shouldTrackAuthenticatedUserByEmail() throws Exception {
        properties.getAuthenticated().setCapacity(2);
        properties.getAuthenticated().setRefillTokensPerMinute(1);

        // 2 requests allowed
        mockMvc.perform(get("/api/v1/listings/search")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/listings/search")).andExpect(status().isOk());

        // 3rd request rejected
        mockMvc.perform(get("/api/v1/listings/search"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }
}