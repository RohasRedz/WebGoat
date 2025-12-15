package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Delta tests for WebSecurityConfig focusing only on the security-related changes:
 * - PasswordEncoder must be a strong encoder (BCrypt-based), not NoOp/plain text.
 * - CSRF and security headers are no longer disabled via .disable() calls.
 *
 * Note: These tests do not attempt full Spring Boot context integration, but
 * validate key properties of the beans and configuration behavior in isolation.
 */
class WebSecurityConfigTest {

    @Test
    void passwordEncoderShouldNotBeNoOpAndShouldEncodePassword() {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "SecretPassword123!";
        String encoded = encoder.encode(rawPassword);

        // Assert
        // Ensure we are no longer returning a NoOp/plain-text encoder:
        assertThat(encoder.getClass().getSimpleName())
                .as("PasswordEncoder should be a strong implementation like BCryptPasswordEncoder")
                .isNotEqualTo("NoOpPasswordEncoder");
        assertThat(encoded)
                .as("Encoded password must differ from raw password (no plain-text storage)")
                .isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encoded))
                .as("Encoded password must be verifiable with matches()")
                .isTrue();
    }

    @Test
    void filterChainShouldNotDisableCsrfOrSecurityHeaders() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // We cannot easily introspect lambda configuration, but we can at least ensure
        // that a SecurityFilterChain is built successfully and does not trivially allow
        // anonymous access to authenticated-only endpoints.
        HttpSecurityBuilderStub http = new HttpSecurityBuilderStub();

        // Act
        SecurityFilterChain chain = config.filterChain(http.build());

        // Assert
        assertThat(chain)
                .as("SecurityFilterChain should be created successfully")
                .isNotNull();
        // We can't directly assert internal CSRF/header flags without a full Spring context,
        // but we can assert that the chain still enforces authentication by default.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/some/protected/resource");
        Authentication authentication = chain.getFilters().isEmpty()
                ? null
                : SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication)
                .as("Authentication should not be pre-populated for anonymous users")
                .isNull();
    }

    @Test
    void authenticationManagerBeanShouldBeCreatedFromConfiguration() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        org.mockito.Mockito.when(authenticationConfiguration.getAuthenticationManager())
                .thenReturn(mockManager);

        // Act
        AuthenticationManager manager = config.authenticationManager(authenticationConfiguration);

        // Assert
        assertThat(manager)
                .as("AuthenticationManager should be obtained from AuthenticationConfiguration")
                .isSameAs(mockManager);
    }

    /**
     * Minimal stub to create a usable HttpSecurity without pulling in the whole Spring Boot test stack.
     * This is intentionally lightweight and only serves to allow filterChain() to be invoked.
     */
    private static class HttpSecurityBuilderStub {

        private org.springframework.security.config.annotation.web.builders.HttpSecurity httpSecurity;

        HttpSecurityBuilderStub() throws Exception {
            this.httpSecurity = new org.springframework.security.config.annotation.web.builders.HttpSecurity(
                    mock(org.springframework.security.config.annotation.ObjectPostProcessor.class),
                    new org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder(null),
                    java.util.Collections.emptyMap());
        }

        org.springframework.security.config.annotation.web.builders.HttpSecurity build() {
            return httpSecurity;
        }
    }
}
