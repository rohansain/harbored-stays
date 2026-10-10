package com.rohan.harbored.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long testExpiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(testSecret, testExpiration);
    }

    @Test
    @DisplayName("Should generate a valid token and extract username correctly")
    void shouldGenerateAndValidateToken() {
        UserDetails userDetails = new User(
                "rohan@harbored.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"))
        );

        String token = jwtUtil.generateToken(userDetails, 101L, "GUEST");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("rohan@harbored.com");
        assertThat(jwtUtil.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("Should invalidate token when username does not match")
    void shouldInvalidateTokenOnUsernameMismatch() {
        UserDetails originalUser = new User(
                "rohan@harbored.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"))
        );
        UserDetails differentUser = new User(
                "attacker@harbored.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"))
        );

        String token = jwtUtil.generateToken(originalUser, 101L, "GUEST");

        assertThat(jwtUtil.isTokenValid(token, differentUser)).isFalse();
    }
}