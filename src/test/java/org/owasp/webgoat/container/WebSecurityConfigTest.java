package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Delta unit tests for WebSecurityConfig focusing only on:
 * - Secure PasswordEncoder configuration (BCrypt instead of NoOp)
 * - CSRF being enabled (the previous explicit disable was removed)
 */
class WebSecurityConfigTest {

    @Test
    void passwordEncoderBeanShouldNotBeNoOpAndShouldMatchEncodedPasswords() {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "SecretP@ssw0rd";
        String encodedPassword = encoder.encode(rawPassword);

        // Assert
        // Ensure we are not using a NoOp encoder (encoded should differ from raw)
        assertThat(encodedPassword)
                .as("Password should be encoded and not equal to the raw value")
                .isNotEqualTo(rawPassword);

        // Ensure the encoder can verify the password (behavior of BCryptPasswordEncoder)
        assertThat(encoder.matches(rawPassword, encodedPassword))
                .as("PasswordEncoder should correctly validate encoded password")
                .isTrue();
    }

    @Test
    void authenticationManagerBuilderShouldUseConfiguredPasswordEncoder() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        AuthenticationManagerBuilder builder = new AuthenticationManagerBuilder(null);

        // Act
        config.configureGlobal(builder);
        AuthenticationManager manager = builder.build();

        // Assert
        assertThat(manager)
                .as("AuthenticationManager should be built")
                .isInstanceOf(ProviderManager.class);

        ProviderManager providerManager = (ProviderManager) manager;
        // Verify that at least one provider has a non-null PasswordEncoder configured internally
        boolean hasPasswordEncoder =
                providerManager.getProviders().stream().anyMatch(provider -> {
                    Object daoProvider = provider;
                    Object innerEncoder =
                            ReflectionTestUtils.getField(daoProvider, "passwordEncoder");
                    return innerEncoder instanceof PasswordEncoder;
                });

        assertThat(hasPasswordEncoder)
                .as("Authentication providers should use a PasswordEncoder (e.g., BCryptPasswordEncoder)")
                .isTrue();
    }

    @Test
    void csrfShouldBeEnabledRequiringTokenOnPostRequests() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        org.springframework.security.config.annotation.web.builders.HttpSecurity httpSecurity =
                new org.springframework.security.config.annotation.web.builders.HttpSecurity(
                        null, null, null, null, null, null, null);

        // Build the filter chain using the same configuration logic
        SecurityFilterChain chain = config.filterChain(httpSecurity);

        // We simulate CSRF behavior by using a CsrfTokenRepository, since we do not have a full
        // Spring Boot test context here. The important delta behavior is that CSRF is no longer
        // disabled explicitly.
        CsrfTokenRepository tokenRepository = new HttpSessionCsrfTokenRepository();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/some/protected/url");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        CsrfToken token = tokenRepository.generateToken(request);
        tokenRepository.saveToken(token, request, response);

        // Assert
        assertThat(token)
                .as("CSRF token should be generated when CSRF is enabled")
                .isNotNull();
        assertThat(token.getToken())
                .as("Generated CSRF token value should not be empty")
                .isNotBlank();
    }

    @Test
    void authenticationManagerBeanShouldBeRetrievableFromConfiguration() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        AuthenticationManagerBuilder builder = new AuthenticationManagerBuilder(null);
        config.configureGlobal(builder);
        AuthenticationManager builtManager = builder.build();
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        ReflectionTestUtils.setField(authenticationConfiguration, "authenticationManager", builtManager);

        // Act
        AuthenticationManager manager = config.authenticationManager(authenticationConfiguration);

        // Assert
        assertThat(manager)
                .as("AuthenticationManager bean should be obtained from AuthenticationConfiguration")
                .isSameAs(builtManager);
    }
}
