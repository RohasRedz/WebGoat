package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Delta unit tests for WebSecurityConfig focusing only on the securityrelevant changes:
 *
 * - CSRF must now be enabled and a BCryptbased PasswordEncoder bean is present.
 */
public class WebSecurityConfigSecurityDeltaTest {

    /**
     * Verifies that the passwordEncoder bean is present and is a BCryptPasswordEncoder,
     * ensuring that plaintext/no-op encoders are no longer used.
     */
    @Test
    @DisplayName("passwordEncoder bean should be a BCryptPasswordEncoder")
    void passwordEncoderShouldUseBCrypt() {
        // Arrange
        UserService userService = org.mockito.Mockito.mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        assertThat(encoder)
                .as("PasswordEncoder bean should be an instance of BCryptPasswordEncoder")
                .isInstanceOf(BCryptPasswordEncoder.class);

        // Sanity: encoding should not echo the raw password (basic safeguard against NoOp)
        String raw = "test-password";
        String encoded = encoder.encode(raw);
        assertThat(encoded).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, encoded)).isTrue();
    }

    /**
     * Minimal smoke test to assert that the userDetailsServiceBean is still wired correctly
     * and returns the same UserService instance passed into the configuration.
     */
    @Test
    @DisplayName("userDetailsServiceBean should return injected UserService")
    void userDetailsServiceBeanShouldReturnInjectedService() {
        // Arrange
        UserService userService = org.mockito.Mockito.mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        UserDetailsService uds = config.userDetailsServiceBean();

        // Assert
        assertThat(uds).isSameAs(userService);
    }
}
