// Assuming the project uses the same package structure for tests as for main sources.
// If this does not match your project layout, adjust the package below accordingly.
package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Delta tests for WebSecurityConfig focusing only on:
 * 1) CSRF protection is no longer disabled.
 * 2) A strong PasswordEncoder (BCrypt) is used instead of NoOpPasswordEncoder/plain-text.
 */
class WebSecurityConfigDeltaTest {

    /**
     * Verifies that the configured PasswordEncoder bean is NOT a no-op/plain-text encoder and
     * that it actually hashes passwords.
     */
    @Test
    void passwordEncoder_shouldHashPasswordsAndNotBePlainText() {
        // Arrange
        UserDetailsService dummyUserService = username -> null; // Not used by this test
        WebSecurityConfig config = new WebSecurityConfig((org.owasp.webgoat.container.users.UserService) dummyUserService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "SecretPassword123!";
        String encodedPassword = encoder.encode(rawPassword);

        // Assert
        // Encoded password should not be null or equal to the raw password
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);

        // Encoded password should match via PasswordEncoder#matches
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();

        // Encoding the same password twice should typically produce different hashes with BCrypt
        String encodedPassword2 = encoder.encode(rawPassword);
        assertThat(encodedPassword2).isNotEqualTo(encodedPassword);
    }

    /**
     * Verifies that CSRF protection is not explicitly disabled in the SecurityFilterChain.
     * This is a behavior-oriented test: we confirm that the CSRF configuration remains enabled
     * (i.e., the config does not call csrf().disable()).
     *
     * NOTE: This uses a mock HttpSecurity and checks that we never invoke "disable()" on CsrfConfigurer.
     * If the internals of Spring Security change, this test may need to be updated.
     */
    @Test
    void filterChain_shouldNotDisableCsrfProtection() throws Exception {
        // Arrange
        HttpSecurity http = org.mockito.Mockito.mock(HttpSecurity.class);
        CsrfConfigurer<HttpSecurity> csrfConfigurer = org.mockito.Mockito.mock(CsrfConfigurer.class);

        // Stub http.csrf() to return our csrfConfigurer mock
        org.mockito.Mockito.when(http.csrf(org.mockito.Mockito.any())).thenAnswer(invocation -> {
            java.util.function.Consumer<CsrfConfigurer<HttpSecurity>> consumer =
                    invocation.getArgument(0);
            consumer.accept(csrfConfigurer);
            return http;
        });

        UserDetailsService dummyUserService = username -> null; // Not used for CSRF config
        WebSecurityConfig config = new WebSecurityConfig((org.owasp.webgoat.container.users.UserService) dummyUserService);

        // Act
        SecurityFilterChain filterChain = config.filterChain(http);

        // Assert
        assertThat(filterChain).isNotNull();

        // Ensure that disable() was never called on the CsrfConfigurer.
        // This tests the changed behavior from explicitly disabling CSRF to keeping it enabled.
        org.mockito.Mockito.verify(csrfConfigurer, org.mockito.Mockito.never()).disable();
    }
}
