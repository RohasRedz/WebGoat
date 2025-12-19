package org.owasp.webgoat.container;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Delta unit tests for WebSecurityConfig focusing only on the security-related changes:
 * 1) PasswordEncoder now uses BCryptPasswordEncoder.
 * 2) CSRF is not explicitly disabled (default CSRF protection is active).
 * 3) Security headers are configured instead of being globally disabled.
 */
class WebSecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder bean should return BCryptPasswordEncoder")
    void passwordEncoderShouldUseBCrypt() {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(null); // userDetailsService not needed here

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        assertThat(encoder)
            .as("PasswordEncoder should be an instance of BCryptPasswordEncoder")
            .isInstanceOf(BCryptPasswordEncoder.class);

        // Additionally verify that encoding changes the raw value (not NoOp)
        String raw = "test-password";
        String encoded = encoder.encode(raw);
        assertThat(encoded)
            .as("Encoded password should not equal raw password")
            .isNotEqualTo(raw);
        assertThat(encoder.matches(raw, encoded))
            .as("BCryptPasswordEncoder should correctly verify the encoded password")
            .isTrue();
    }

    @Test
    @DisplayName("SecurityFilterChain should not disable CSRF and should issue a CSRF token for unsafe methods")
    void csrfShouldBeEnabledByDefault() throws Exception {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(null);
        HttpSecurityBuilderFactory builderFactory = new HttpSecurityBuilderFactory();
        SecurityFilterChain chain = config.filterChain(builderFactory.create());

        // Act: perform a POST request which, with CSRF enabled, should result
        // in a CSRF token being generated and attached to the response.
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/some/protected/url");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain servletChain = mock(FilterChain.class);

        chain.doFilter(request, response, servletChain);

        // Assert
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        assertThat(csrfToken)
            .as("CSRF token should be present when CSRF protection is enabled")
            .isNotNull();
        assertThat(csrfToken.getToken())
            .as("CSRF token value should not be empty")
            .isNotBlank();
    }

    @Test
    @DisplayName("Security headers should be set: X-Frame-Options, X-Content-Type-Options, HSTS, Referrer-Policy, CSP")
    void securityHeadersShouldBeConfigured() throws Exception {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(null);
        HttpSecurityBuilderFactory builderFactory = new HttpSecurityBuilderFactory();
        SecurityFilterChain chain = config.filterChain(builderFactory.create());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/some/protected/url");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain servletChain = mock(FilterChain.class);

        // Act
        chain.doFilter(request, response, servletChain);

        // Assert: verify key security headers that were configured in the fix
        assertThat(response.getHeader("X-Frame-Options"))
            .as("X-Frame-Options should be DENY")
            .isEqualTo("DENY");

        assertThat(response.getHeader("X-Content-Type-Options"))
            .as("X-Content-Type-Options should be nosniff")
            .isEqualToIgnoringCase("nosniff");

        String hsts = response.getHeader("Strict-Transport-Security");
        assertThat(hsts)
            .as("HSTS should be configured with includeSubDomains and a max-age")
            .isNotNull()
            .containsIgnoringCase("max-age=")
            .containsIgnoringCase("includeSubDomains");

        String referrerPolicy = response.getHeader("Referrer-Policy");
        assertThat(referrerPolicy)
            .as("Referrer-Policy should be STRICT_ORIGIN_WHEN_CROSS_ORIGIN")
            .isEqualToIgnoringCase("strict-origin-when-cross-origin");

        String csp = response.getHeader("Content-Security-Policy");
        assertThat(csp)
            .as("Content-Security-Policy should be configured as in WebSecurityConfig")
            .contains("default-src 'self'")
            .contains("script-src 'self' 'unsafe-inline'")
            .contains("style-src 'self' 'unsafe-inline'");
    }

    /**
     * Small helper to create an HttpSecurity instance without wiring the entire Spring context.
     *
     * This class isolates boilerplate needed to construct an HttpSecurity for testing the
     * SecurityFilterChain produced by WebSecurityConfig.
     */
    private static final class HttpSecurityBuilderFactory {

        SecurityFilterChain create() throws Exception {
            // We create a minimal HttpSecurity using a mocked AuthenticationConfiguration.
            AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
            // We don't rely on the real AuthenticationManager in these tests.
            when(authConfig.getAuthenticationManager()).thenReturn(mock(org.springframework.security.authentication.AuthenticationManager.class));

            org.springframework.security.config.annotation.web.builders.HttpSecurity http =
                new org.springframework.security.config.annotation.web.builders.HttpSecurity(
                    null, null, java.util.Collections.emptyMap());

            // Build the filter chain via the provided WebSecurityConfig.filterChain method
            WebSecurityConfig config = new WebSecurityConfig(null);
            return config.filterChain(http);
        }
    }
}
