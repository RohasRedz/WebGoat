package org.owasp.webgoat.container;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Delta unit tests for WebSecurityConfig focusing only on:
 * 1) CSRF is not disabled anymore (i.e., no explicit csrf.disable() is applied).
 * 2) AuthenticationManagerBuilder is configured to use a PasswordEncoder (BCryptPasswordEncoder).
 *
 * These tests are intentionally scoped to the behavior changed by the security fix.
 */
public class WebSecurityConfigDeltaTest {

    @Test
    @DisplayName("filterChain should not disable CSRF explicitly")
    void filterChain_shouldNotDisableCsrf() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        @SuppressWarnings("unchecked")
        HttpSecurity http = mock(HttpSecurity.class);

        // Mocks for the fluent API
        HttpSecurity.AuthorizeHttpRequestsConfigurer authorizeConfigurer =
                mock(HttpSecurity.AuthorizeHttpRequestsConfigurer.class, RETURNS_SELF);
        HttpSecurity.FormLoginConfigurer formLoginConfigurer =
                mock(HttpSecurity.FormLoginConfigurer.class, RETURNS_SELF);
        HttpSecurity.OAuth2LoginConfigurer oauth2LoginConfigurer =
                mock(HttpSecurity.OAuth2LoginConfigurer.class, RETURNS_SELF);
        HttpSecurity.LogoutConfigurer logoutConfigurer =
                mock(HttpSecurity.LogoutConfigurer.class, RETURNS_SELF);
        HeadersConfigurer<HttpSecurity> headersConfigurer =
                mock(HeadersConfigurer.class, RETURNS_SELF);
        CsrfConfigurer<HttpSecurity> csrfConfigurer =
                mock(CsrfConfigurer.class, RETURNS_SELF);
        HttpSecurity.ExceptionHandlingConfigurer exceptionHandlingConfigurer =
                mock(HttpSecurity.ExceptionHandlingConfigurer.class, RETURNS_SELF);
        SecurityFilterChain mockChain = mock(SecurityFilterChain.class);

        // Stubbing for fluent methods used in filterChain
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.formLogin(any())).thenReturn(http);
        when(http.oauth2Login(any())).thenReturn(http);
        when(http.logout(any())).thenReturn(http);
        when(http.headers(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http); // We want to see if this is *called* at all
        when(http.exceptionHandling(any())).thenReturn(http);
        when(http.build()).thenReturn(mockChain);

        // Act
        SecurityFilterChain result = config.filterChain(http);

        // Assert
        assertNotNull(result, "SecurityFilterChain should be created");

        // Capture how csrf() was configured, if at all
        ArgumentCaptor<java.util.function.Consumer<CsrfConfigurer<HttpSecurity>>> csrfConsumerCaptor =
                ArgumentCaptor.forClass(java.util.function.Consumer.class);

        // Verify if csrf(...) was ever invoked; if the fix only removed csrf.disable(),
        // it might not be called at all, which is acceptable as CSRF is enabled by default.
        verify(http, atMostOnce()).csrf(csrfConsumerCaptor.capture());

        if (!csrfConsumerCaptor.getAllValues().isEmpty()) {
            java.util.function.Consumer<CsrfConfigurer<HttpSecurity>> csrfConsumer =
                    csrfConsumerCaptor.getValue();
            assertNotNull(csrfConsumer, "CSRF customizer should not be null if csrf() is invoked");

            // Now verify that the consumer does NOT call csrf.disable()
            // We do this by applying the consumer to a mocked CsrfConfigurer
            CsrfConfigurer<HttpSecurity> mockedCsrf = mock(CsrfConfigurer.class, RETURNS_SELF);
            csrfConsumer.accept(mockedCsrf);

            // If disable() were called inside the consumer, verify(mockedCsrf).disable() would succeed.
            // We assert the opposite: disable() must not be called.
            verify(mockedCsrf, never()).disable();
        }
        // If csrf() is never called, we accept the default-secure behavior as valid,
        // so there is nothing more to assert in that case.
    }

    @Test
    @DisplayName("configureGlobal should configure AuthenticationManagerBuilder with a PasswordEncoder")
    void configureGlobal_shouldUsePasswordEncoder() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        AuthenticationManagerBuilder authBuilder = mock(AuthenticationManagerBuilder.class, RETURNS_SELF);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        // Act
        config.configureGlobal(authBuilder, passwordEncoder);

        // Assert
        // Verify that the userDetailsService is registered
        verify(authBuilder).userDetailsService(userService);
        // Verify that the same PasswordEncoder instance provided is wired into the builder
        verify(authBuilder).passwordEncoder(passwordEncoder);
    }

    @Test
    @DisplayName("passwordEncoder bean should be a BCryptPasswordEncoder")
    void passwordEncoderBean_shouldReturnBCryptPasswordEncoder() {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        assertNotNull(encoder, "PasswordEncoder bean must not be null");
        assertTrue(encoder instanceof BCryptPasswordEncoder,
                "PasswordEncoder must be an instance of BCryptPasswordEncoder to avoid plain-text or weak encoding");
    }

    @Test
    @DisplayName("userDetailsServiceBean should return injected UserService instance (regression guard around configureGlobal change)")
    void userDetailsServiceBean_shouldReturnInjectedUserService() {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        UserDetailsService userDetailsService = config.userDetailsServiceBean();

        // Assert
        assertSame(userService, userDetailsService,
                "userDetailsServiceBean should still return the injected UserService after the security-related changes");
    }

    @Test
    @DisplayName("authenticationManager bean creation should still be delegated to AuthenticationConfiguration")
    void authenticationManagerBean_shouldDelegateToAuthenticationConfiguration() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        AuthenticationManager expectedManager = mock(AuthenticationManager.class);
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(expectedManager);

        // Act
        AuthenticationManager result = config.authenticationManager(authenticationConfiguration);

        // Assert
        assertSame(expectedManager, result,
                "authenticationManager bean must still delegate to AuthenticationConfiguration after the fix");
        verify(authenticationConfiguration).getAuthenticationManager();
    }
}
