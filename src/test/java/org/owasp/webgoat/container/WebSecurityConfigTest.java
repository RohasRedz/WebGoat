package org.owasp.webgoat.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.util.ReflectionTestUtils;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Delta unit tests for WebSecurityConfig focusing only on:
 * - Replacement of NoOpPasswordEncoder with BCryptPasswordEncoder
 * - CSRF configuration using CookieCsrfTokenRepository
 *
 * These tests avoid asserting unrelated behavior in the class.
 */
public class WebSecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder should return a BCryptPasswordEncoder instead of NoOpPasswordEncoder")
    void passwordEncoder_usesBCryptEncoder() {
        // Arrange
        UserService mockUserService = org.mockito.Mockito.mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(mockUserService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        // The vulnerability fix requires using a secure password encoder implementation
        assertTrue(encoder instanceof BCryptPasswordEncoder,
                "passwordEncoder() must return a BCryptPasswordEncoder to avoid plain-text or weak encoders");

        String rawPassword = "secret123!";
        String encoded1 = encoder.encode(rawPassword);
        String encoded2 = encoder.encode(rawPassword);

        // Assert encoded password is not equal to raw and matches correctly
        assertTrue(encoder.matches(rawPassword, encoded1),
                "Encoded password must match the raw password using BCryptPasswordEncoder");
        // BCrypt should be salted, so multiple encodes should not usually be equal
        assertTrue(!encoded1.equals(encoded2),
                "Consecutive encodings of the same password should typically differ due to salting");
    }

    @Test
    @DisplayName("filterChain should configure CSRF with CookieCsrfTokenRepository and not disable it")
    void filterChain_enablesCsrfWithCookieCsrfTokenRepository() throws Exception {
        // Arrange
        UserService mockUserService = org.mockito.Mockito.mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(mockUserService);
        HttpSecurity http = org.springframework.security.config.Customizer
                .withDefaults()
                .apply(new HttpSecurity(new org.springframework.security.config.annotation.ObjectPostProcessor<>() {
                    @Override
                    public <O> O postProcess(O object) {
                        return object;
                    }
                }, new AuthenticationConfiguration() {
                    @Override
                    public AuthenticationManager getAuthenticationManager() {
                        return null; // Not required for CSRF configuration validation in this delta test
                    }
                }, java.util.Collections.emptyMap()));

        // Act
        SecurityFilterChain chain = config.filterChain(http);

        // Assert
        // We cannot easily introspect the internal CSRF configuration without full Spring context,
        // so we use reflection to verify that a CookieCsrfTokenRepository is part of the configuration.
        Object csrfConfigurer = ReflectionTestUtils.getField(http, "csrf");
        // In newer Spring versions csrfConfigurer may be null if built into the filter chain; this test
        // focuses on ensuring the configuration method has not been
        // reverted to csrf.disable(), by inspecting the method body indirectly:
        String configSource = WebSecurityConfig.class.getDeclaredMethod("filterChain", HttpSecurity.class)
                .toString();
        assertTrue(configSource.contains("CookieCsrfTokenRepository"),
                "filterChain() must configure CSRF using CookieCsrfTokenRepository instead of disabling CSRF");

        // Also ensure that a SecurityFilterChain was built successfully as a sanity check
        assertEquals(SecurityFilterChain.class, chain.getClass(),
                "filterChain() must return a SecurityFilterChain instance");
    }

    @Test
    @DisplayName("userDetailsServiceBean should still return the injected UserService after refactor")
    void userDetailsServiceBean_returnsInjectedUserService() {
        // Arrange
        UserService mockUserService = org.mockito.Mockito.mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(mockUserService);

        // Act
        UserDetailsService userDetailsService = config.userDetailsServiceBean();

        // Assert
        // This ensures that the security fix around the PasswordEncoder did not break the
        // wiring of the UserDetailsService that authentication relies on.
        assertEquals(mockUserService, userDetailsService,
                "userDetailsServiceBean() must return the same UserService instance that was injected");
    }
}
