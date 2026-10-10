package com.rohan.harbored.service.impl;

import com.rohan.harbored.dto.request.LoginRequestDto;
import com.rohan.harbored.dto.request.SignupRequestDto;
import com.rohan.harbored.dto.response.UserResponseDto;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.exception.BadRequestException;
import com.rohan.harbored.exception.UnauthorizedException;
import com.rohan.harbored.mapper.UserMapper;
import com.rohan.harbored.repository.UserRepository;
import com.rohan.harbored.security.CustomUserDetails;
import com.rohan.harbored.security.JwtUtil;
import com.rohan.harbored.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.harbored.dto.response.AuthResponseDto;

import java.net.Authenticator;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public UserResponseDto signup(SignupRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toResponseDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        final Authentication authentication;
        try {
            // 1. Passes unauthenticated credentials into ProviderManager (AuthenticationManager implementation).
            // 2. ProviderManager iterates registered providers and delegates to DaoAuthenticationProvider.
            // 3. DaoAuthenticationProvider calls CustomUserDetailsService.loadUserByUsername(...) to fetch DB hash.
            // 4. BCryptPasswordEncoder compares the raw request password against the retrieved database hash.
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );
        } catch (BadRequestException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // 1. Retrieve the authenticated principal populated by DaoAuthenticationProvider during authentication
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 2. Extract the primary granted authority/role string (e.g., "ROLE_USER") from the user's authorities collection
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        // 3. Mint the signed JWT containing standard subject claims along with custom claims (userId, role)
        String token = jwtUtil.generateToken(
                userDetails,
                userDetails.getId(),
                role
        );

        // 4. Fetch the database entity to populate full profile attributes for the response DTO
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // 5. Transform entity data into a sanitized response DTO and return the token with Bearer scheme
        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toResponseDto(user))
                .build();
    }
}