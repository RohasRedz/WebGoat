package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Delta unit tests for {@link WebSecurityConfig}:
 * Verifies only the security-sensitive behavior that changed:
 * 1) Password encoder bean now uses BCryptPasswordEncoder.
 */
public class WebSecurityConfigTest {

    @Test
    void passwordEncoder_shouldUseBCryptAndNotBeNoOp() {
        // Arrange
        UserService dummyUserService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(dummyUserService);

        // Act
        BCryptPasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "secret-password";
        String encoded = encoder.encode(rawPassword);

        // Assert
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        // BCrypt should not return the raw password
        assertThat(encoded).isNotEqualTo(rawPassword);
        // Encoded password should match via BCrypt's verify method
        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
    }
}
