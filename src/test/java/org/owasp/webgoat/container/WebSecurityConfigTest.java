package org.owasp.webgoat.container;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Delta unit tests for WebSecurityConfig focusing only on:
 * 1) CSRF configuration change (no longer disabled, now using CookieCsrfTokenRepository.withHttpOnlyFalse()).
 * 2) PasswordEncoder bean change (NoOpPasswordEncoder -> BCryptPasswordEncoder).
 *
 * These tests intentionally avoid covering unrelated behavior.
 */
class WebSecurityConfigTest {

    private WebSecurityConfig webSecurityConfig;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        webSecurityConfig = new WebSecurityConfig(userService);
    }

    @Test
    void passwordEncoderBeanShouldBeBCryptPasswordEncoder() {
        // Arrange & Act
        PasswordEncoder passwordEncoder = webSecurityConfig.passwordEncoder();

        // Assert
        assertNotNull(passwordEncoder, "PasswordEncoder bean must not be null");
        assertTrue(passwordEncoder instanceof BCryptPasswordEncoder,
                "PasswordEncoder bean must be an instance of BCryptPasswordEncoder to avoid plaintext or weak encoding");
    }

    @Test
    void filterChainShouldConfigureCsrfWithCookieCsrfTokenRepository() throws Exception {
        // Arrange
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);

        @SuppressWarnings("unchecked")
        CsrfConfigurer<HttpSecurity> csrfConfigurer = mock(CsrfConfigurer.class, RETURNS_DEEP_STUBS);
        when(http.csrf(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<CsrfConfigurer<HttpSecurity>> configurerConsumer =
                    (Consumer<CsrfConfigurer<HttpSecurity>>) invocation.getArgument(0);
            configurerConsumer.accept(csrfConfigurer);
            return http;
        });

        // The following stubbings are only to satisfy the fluent API used in filterChain and keep the test
        // focused on CSRF behavior without NPEs or chained-call issues.
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.formLogin(any())).thenReturn(http);
        when(http.oauth2Login(any())).thenReturn(http);
        when(http.logout(any())).thenReturn(http);
        when(http.headers(any())).thenReturn(http);
        when(http.exceptionHandling(any())).thenReturn(http);
        when(http.build()).thenReturn(mock(SecurityFilterChain.class));

        when(csrfConfigurer.csrfTokenRepository(any())).thenReturn(csrfConfigurer);

        // Act
        webSecurityConfig.filterChain(http);

        // Assert
        verify(http, times(1)).csrf(any());
        verify(csrfConfigurer, times(1))
                .csrfTokenRepository(argThat(repo ->
                        repo instanceof CookieCsrfTokenRepository &&
                                // We cannot directly assert HttpOnly flag here without accessing internal state,
                                // but verifying type correctness ensures use of CookieCsrfTokenRepository.
                                true
                ));
    }
}
