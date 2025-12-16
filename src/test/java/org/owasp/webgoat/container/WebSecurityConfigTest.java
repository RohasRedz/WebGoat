// Delta_UnitTest_Agent
// Package inferred from source file location; adjust if project structure differs.
package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Delta tests for WebSecurityConfig focusing ONLY on the changed behavior:
 *  - No longer using NoOpPasswordEncoder; now uses BCryptPasswordEncoder.
 *  - CSRF and headers are no longer disabled; default configuration is applied.
 */
class WebSecurityConfigTest {

    @Test
    void passwordEncoder_shouldUseBCryptPasswordEncoder() {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(null /* UserService is not used here */);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        assertThat(encoder)
                .as("passwordEncoder() must return a BCryptPasswordEncoder instead of NoOpPasswordEncoder")
                .isInstanceOf(BCryptPasswordEncoder.class);

        // and verify that it actually hashes (i.e., does not return raw password)
        String rawPassword = "secret";
        String encoded = encoder.encode(rawPassword);
        assertThat(encoded)
                .as("Encoded password should differ from raw password to avoid plain-text storage")
                .isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encoded))
                .as("Encoder should successfully verify the raw password against the hash")
                .isTrue();
    }

    @Test
    void filterChain_shouldNotDisableCsrfOrHeaders() throws Exception {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(null);

        HttpSecurity http = new HttpSecurity(null, null, null, null, null, null, null);

        // Act
        SecurityFilterChain chain = config.filterChain(http);

        // Assert basic creation
        assertThat(chain)
                .as("SecurityFilterChain must be created successfully")
                .isNotNull();

        // Delta assertions (behavior that changed):
        // We verify that the CSRF and headers configurers are present and not explicitly disabled.
        CsrfConfigurer<HttpSecurity> csrf = http.getConfigurer(CsrfConfigurer.class);
        HeadersConfigurer<HttpSecurity> headers = http.getConfigurer(HeadersConfigurer.class);

        assertThat(csrf)
                .as("CSRF configuration should be enabled via Customizer.withDefaults() and not disabled")
                .isNotNull();
        assertThat(headers)
                .as("Security headers configuration should be enabled via Customizer.withDefaults() and not disabled")
                .isNotNull();

        // Note: We intentionally do NOT assert more granular details to keep the test focused
        // strictly on the change from 'disable()' to default configuration.
    }
}
