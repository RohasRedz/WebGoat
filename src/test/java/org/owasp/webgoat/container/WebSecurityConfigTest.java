package org.owasp.webgoat.container;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Delta tests for WebSecurityConfig focusing on:
 * - Ensuring PasswordEncoder is a strong encoder (BCryptPasswordEncoder).
 * - Ensuring CSRF is enabled and configured with CookieCsrfTokenRepository.
 */
class WebSecurityConfigTest {

    @Test
    void passwordEncoderUsesBCrypt() {
        WebSecurityConfig config = new WebSecurityConfig();

        PasswordEncoder encoder = config.passwordEncoder();

        assertNotNull(encoder, "PasswordEncoder bean must not be null");
        assertTrue(encoder instanceof BCryptPasswordEncoder,
                "PasswordEncoder must be an instance of BCryptPasswordEncoder to avoid plain-text or weak hashing");
        String raw = "secret-password";
        String encoded = encoder.encode(raw);
        assertTrue(encoder.matches(raw, encoded),
                "Encoded password should match the raw password when verified");
    }

    @Test
    void filterChainEnablesCsrfWithCookieRepository() throws Exception {
        WebSecurityConfig config = new WebSecurityConfig();

        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        // Configure mocks for chained calls
        when(http.csrf().csrfTokenRepository(any(CookieCsrfTokenRepository.class)).and())
                .thenReturn(http);
        when(http.authorizeRequests().anyRequest().permitAll())
                .thenReturn(http);
        when(http.build()).thenReturn(mock(SecurityFilterChain.class));

        SecurityFilterChain chain = config.filterChain(http);

        assertNotNull(chain, "SecurityFilterChain must not be null");

        // Verify that CSRF configuration is applied using CookieCsrfTokenRepository
        verify(http.csrf()).csrfTokenRepository(any(CookieCsrfTokenRepository.class));
        verify(http.csrf(), never()).disable();
    }
}
