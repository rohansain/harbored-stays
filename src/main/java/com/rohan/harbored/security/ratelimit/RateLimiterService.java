package com.rohan.harbored.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final RateLimitProperties properties;
    private final Map<String, TokenBucket> bucketRegistry = new ConcurrentHashMap<>();

    public boolean tryConsume(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }

        String clientKey = resolveClientKey(request);
        boolean authenticated = isAuthenticated();

        TokenBucket bucket = bucketRegistry.computeIfAbsent(clientKey, key -> createBucket(authenticated));
        boolean allowed = bucket.tryConsume();

        if (!allowed) {
            log.warn("Rate limit exceeded for client key: {} (Authenticated: {})", clientKey, authenticated);
        }

        return allowed;
    }

    private TokenBucket createBucket(boolean authenticated) {
        RateLimitProperties.LimitConfig config = authenticated
                ? properties.getAuthenticated()
                : properties.getAnonymous();

        return new TokenBucket(config.getCapacity(), config.getRefillTokensPerMinute(), TimeUnit.MINUTES);
    }

    public String resolveClientKey(HttpServletRequest request) {
        if (isAuthenticated()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return "user:" + authentication.getName().toLowerCase().trim();
        }

        return "ip:" + extractClientIp(request);
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    public void clearRegistry() {
        bucketRegistry.clear();
    }
}