package com.rohan.harbored.repository;

import com.rohan.harbored.entity.User;
import com.rohan.harbored.entity.enums.Role;
import com.rohan.harbored.entity.enums.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save a user and fetch by email successfully")
    void shouldSaveAndFetchUserByEmail() {
        User user = User.builder()
                .name("Rohan Tech")
                .email("test.engineer@harbored.com")
                .passwordHash("$2a$10$abcdefghijklmnopqrstuvwxyz12345678901234567890123456")
                .role(Role.GUEST)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .build();

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();

        Optional<User> fetchedUser = userRepository.findByEmail("test.engineer@harbored.com");
        assertThat(fetchedUser).isPresent();
        assertThat(fetchedUser.get().getEmail()).isEqualTo("test.engineer@harbored.com");
        assertThat(fetchedUser.get().getRole()).isEqualTo(Role.GUEST);
        assertThat(fetchedUser.get().getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
    }
}
