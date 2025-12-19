// Assumed package based on source file path; adjust if actual package differs.
package org.owasp.webgoat.container;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Delta tests for WebSecurityConfig focusing only on the security‑related changes:
 * - Use of BCryptPasswordEncoder instead of NoOpPasswordEncoder.
 * - CSRF is no longer explicitly disabled (i.e., left enabled by default).
 */
class WebSecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder bean must be a BCryptPasswordEncoder")
    void passwordEncoderShouldUseBCrypt() {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        assertNotNull(encoder, "PasswordEncoder bean must not be null");
        assertTrue(encoder instanceof BCryptPasswordEncoder,
                "PasswordEncoder must be an instance of BCryptPasswordEncoder to avoid plain-text encoding");
        // Basic behavior sanity check to ensure it actually encodes and matches
        String raw = "secret";
        String encoded = encoder.encode(raw);
        assertNotEquals(raw, encoded, "Encoded password must not equal the raw password");
        assertTrue(encoder.matches(raw, encoded), "PasswordEncoder must validate the encoded password correctly");
    }

    @Test
    @DisplayName("filterChain must build successfully with CSRF not explicitly disabled")
    void filterChainShouldBuildAndNotDisableCsrf() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);

        // We want to ensure that the configuration does NOT call csrf(csrf -> csrf.disable()) anymore.
        // To do this in a delta test, we verify that the csrf() method is never invoked at all
        // during filterChain setup, which reflects the new behavior of relying on Spring defaults.
        when(http.authorizeHttpRequests(any()))
                .thenReturn(http);
        when(http.formLogin(any()))
                .thenReturn(http);
        when(http.oauth2Login(any()))
                .thenReturn(http);
        when(http.logout(any()))
                .thenReturn(http);
        when(http.headers(any()))
                .thenReturn(http);
        when(http.exceptionHandling(any()))
                .thenReturn(http);
        when(http.build())
                .thenReturn(mock(SecurityFilterChain.class));

        // Act
        SecurityFilterChain chain = config.filterChain(http);

        // Assert
        assertNotNull(chain, "SecurityFilterChain must be created successfully");

        // Verify that csrf() was never called; in the previous vulnerable version, csf.disable() was explicitly configured.
        verify(http, never()).csrf(any());
    }

    @Test
    @DisplayName("Configuration class must be annotated with @EnableWebSecurity")
    void configShouldBeWebSecurityEnabled() {
        // This test ensures that the security configuration class is still active
        // and Spring Security infrastructure remains enabled after the changes.
        EnableWebSecurity annotation = WebSecurityConfig.class.getAnnotation(EnableWebSecurity.class);
        assertNotNull(annotation, "@EnableWebSecurity must be present on WebSecurityConfig");
    }
}
