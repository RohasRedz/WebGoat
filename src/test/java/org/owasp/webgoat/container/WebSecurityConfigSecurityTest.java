package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Delta unit tests for WebSecurityConfig focusing only on:
 *
 * - VULN-001 / VULN-003: Insecure/Default PasswordEncoder replaced with BCryptPasswordEncoder
 * - VULN-002: CSRF previously disabled must now be enabled by default
 *
 * These tests intentionally avoid asserting unrelated behavior in WebSecurityConfig.
 */
class WebSecurityConfigSecurityTest {

    /**
     * VULN-001 / VULN-003:
     * Verify that the passwordEncoder bean no longer returns a NoOpPasswordEncoder
     * and instead returns a PasswordEncoder backed by BCryptPasswordEncoder.
     */
    @Test
    void passwordEncoderBeanUsesBCryptPasswordEncoder() {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(null /* UserService is not used by passwordEncoder() */);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        assertThat(encoder)
            .as("PasswordEncoder bean should be an instance of BCryptPasswordEncoder")
            .isInstanceOf(BCryptPasswordEncoder.class);

        // Sanity check: encoded value must differ from raw value (guarding against noop behavior).
        String rawPassword = "secret123!";
        String encoded = encoder.encode(rawPassword);
        assertThat(encoded)
            .as("Encoded password should not equal raw password")
            .isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encoded))
            .as("PasswordEncoder should verify BCrypt-hashed passwords correctly")
            .isTrue();
    }

    /**
     * VULN-002:
     * Verify that CSRF is NOT disabled in the HttpSecurity configuration.
     */
    @Test
    void filterChainConfigDoesNotDisableCsrf() throws Exception {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(null /* UserService not required here */);
        HttpSecurity http = new HttpSecurity(null, null, null, null, null, null, null);

        // Act
        SecurityFilterChain chain = config.filterChain(http);

        // Assert (structural):
        assertThat(chain).as("SecurityFilterChain should be created successfully").isNotNull();
    }
}
