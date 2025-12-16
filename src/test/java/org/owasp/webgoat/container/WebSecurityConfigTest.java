// Assuming standard Maven-style test package based on source path
package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity.RequestMatcherConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.Mockito.*;

/**
 * Delta tests for WebSecurityConfig focusing only on:
 * - PasswordEncoder change from NoOpPasswordEncoder to BCryptPasswordEncoder
 * - CSRF protection no longer being disabled.
 */
class WebSecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder should not be NoOp and should be a BCrypt-based PasswordEncoder")
    void passwordEncoderShouldUseBCrypt() {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(null); // UserService not needed for this test

        // Act
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "Secret123!";
        String encoded = encoder.encode(rawPassword);

        // Assert
        // Ensure encoder is not null and does not behave like a NoOp encoder (i.e., not equal to raw)
        assertThat(encoder).isNotNull();
        assertThat(encoded).isNotEqualTo(rawPassword);

        // BCrypt hashes should verify correctly
        assertThat(encoder.matches(rawPassword, encoded)).isTrue();

        // And a different password must not match
        assertThat(encoder.matches("otherPassword", encoded)).isFalse();
    }

    @Test
    @DisplayName("filterChain should not explicitly disable CSRF protection")
    void filterChainShouldNotDisableCsrf() throws Exception {
        // Arrange
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        RequestMatcherConfigurer matcherConfigurer = mock(RequestMatcherConfigurer.class, RETURNS_DEEP_STUBS);
        CsrfConfigurer<HttpSecurity> csrfConfigurer = mock(CsrfConfigurer.class);
        HeadersConfigurer<HttpSecurity> headersConfigurer = mock(HeadersConfigurer.class);

        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.formLogin(any())).thenReturn(http);
        when(http.oauth2Login(any())).thenReturn(http);
        when(http.logout(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(csrfConfigurer);
        when(http.headers(any())).thenReturn(headersConfigurer);
        when(http.exceptionHandling(any())).thenReturn(http);
        when(http.build()).thenReturn(mock(SecurityFilterChain.class));

        WebSecurityConfig config = new WebSecurityConfig(null);

        // Act
        config.filterChain(http);

        // Assert
        // Verify that CSRF configuration is never disabled by calling csrf.disable()
        // i.e., filterChain must NOT invoke csrf(csrf -> csrf.disable())
        // So we rely on the contract that the new code no longer calls csrf(...) at all.
        verify(http, never()).csrf(any());

        // Also ensure headers were still configured (regression guard around nearby config)
        verify(http).headers(any());
    }
}
