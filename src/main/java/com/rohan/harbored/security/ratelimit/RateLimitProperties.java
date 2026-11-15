package com.rohan.harbored.security.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ratelimit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;
    private LimitConfig authenticated = new LimitConfig(60, 60);
    private LimitConfig anonymous = new LimitConfig(20, 20);

    @Getter
    @Setter
    public static class LimitConfig {
        private long capacity;
        private long refillTokensPerMinute;

        public LimitConfig() {}

        public LimitConfig(long capacity, long refillTokensPerMinute) {
            this.capacity = capacity;
            this.refillTokensPerMinute = refillTokensPerMinute;
        }
    }
}