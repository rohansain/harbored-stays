package com.rohan.harbored.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohan.harbored.dto.request.CreateListingDto;
import com.rohan.harbored.dto.request.CreateReviewDto;
import com.rohan.harbored.dto.request.LoginRequestDto;
import com.rohan.harbored.dto.request.SignupRequestDto;
import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import com.rohan.harbored.repository.ListingRepository;
import com.rohan.harbored.repository.ReviewRepository;
import com.rohan.harbored.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PhaseZeroIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @BeforeEach
    void cleanDatabase() {
        reviewRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Phase 0 E2E Lifecycle: Signup, Verify, Create Listing, Review, and Cascade Delete")
    void testCompletePhaseZeroFlow() throws Exception {
        // 1. Sign up Host
        SignupRequestDto hostSignup = SignupRequestDto.builder()
                .name("Kavya Host")
                .email("kavya.host@harbored.com")
                .password("Password@123")
                .role(Role.HOST)
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostSignup)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("kavya.host@harbored.com"));

        // 2. Sign up Guest
        SignupRequestDto guestSignup = SignupRequestDto.builder()
                .name("Arjun Guest")
                .email("arjun.guest@harbored.com")
                .password("Password@123")
                .role(Role.GUEST)
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestSignup)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Promote Host to VERIFIED directly in database (simulating admin approval flow)
        User hostEntity = userRepository.findByEmail("kavya.host@harbored.com").orElseThrow();
        hostEntity.setVerificationStatus(VerificationStatus.VERIFIED);
        userRepository.save(hostEntity);

        // 4. Authenticate Host and extract JWT
        LoginRequestDto hostLogin = LoginRequestDto.builder()
                .email("kavya.host@harbored.com")
                .password("Password@123")
                .build();

        MvcResult hostLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andReturn();

        String hostToken = extractToken(hostLoginResult);

        // 5. Authenticate Guest and extract JWT
        LoginRequestDto guestLogin = LoginRequestDto.builder()
                .email("arjun.guest@harbored.com")
                .password("Password@123")
                .build();

        MvcResult guestLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andReturn();

        String guestToken = extractToken(guestLoginResult);

        // 6. Host Creates a Listing
        CreateListingDto listingDto = CreateListingDto.builder()
                .title("Spacious 1BHK near Cyber City")
                .description("Fully furnished flat with high-speed internet and AC, ideal for tech interns.")
                .location("DLF Phase 2")
                .city("Gurugram")
                .pricePerMonth(new BigDecimal("19500.00"))
                .minStayMonths(1)
                .maxStayMonths(6)
                .images(List.of("https://res.cloudinary.com/harbored/image/upload/v1/sample.jpg"))
                .build();

        MvcResult createListingResult = mockMvc.perform(post("/api/v1/listings")
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(listingDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.title").value("Spacious 1BHK near Cyber City"))
                .andReturn();

        Long listingId = extractId(createListingResult);

        // 7. Verify Public Discovery of the Listing (No Auth Header)
        mockMvc.perform(get("/api/v1/listings/" + listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.city").value("Gurugram"))
                .andExpect(jsonPath("$.data.averageRating").value(0.0));

        // 8. Guest Adds a Review
        CreateReviewDto reviewDto = CreateReviewDto.builder()
                .rating(5)
                .comment("Excellent property, zero disturbance, and high speed Wi-Fi.")
                .build();

        mockMvc.perform(post("/api/v1/listings/" + listingId + "/reviews")
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.authorName").value("Arjun Guest"));

        // 9. Confirm Listing Average Rating Recalculation
        mockMvc.perform(get("/api/v1/listings/" + listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(5.0));

        // 10. Delete Listing as Owner and Confirm Cascade Delete of Reviews
        mockMvc.perform(delete("/api/v1/listings/" + listingId)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify database clean up: listing deleted and reviews cascaded
        assertThat(listingRepository.findById(listingId)).isEmpty();
        assertThat(reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId)).isEmpty();
    }

    private String extractToken(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    private Long extractId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }
}