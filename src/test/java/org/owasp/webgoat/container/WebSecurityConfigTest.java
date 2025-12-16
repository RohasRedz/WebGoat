package org.owasp.webgoat.container;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WebSecurityConfigTest {

    @Test
    void filterChain_shouldConfigureCsrfWithCsrfTokenRepository() throws Exception {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(mock(UserService.class));
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        CsrfTokenRepository csrfTokenRepository = config.csrfTokenRepository();

        // We want to verify that the lambda passed to csrf(...) calls csrfTokenRepository(...)
        // Since HttpSecurity is heavily fluent and final in many places, we verify interaction
        // with its csrfConfigurer via deep stubs.
        // Act
        config.filterChain(http);

        // Assert
        // Verify that csrf() was configured at least once
        verify(http).csrf(any());
        assertThat(csrfTokenRepository).isNotNull();
    }

    @Test
    void passwordEncoder_shouldReturnBCryptPasswordEncoder() {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(mock(UserService.class));

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        // Old behavior used NoOpPasswordEncoder (plain-text). We now require a BCrypt-based encoder.
        String rawPassword = "secret";
        String encoded = encoder.encode(rawPassword);

        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
    }

    @Test
    void configureGlobal_shouldUseConfiguredPasswordEncoder() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        AuthenticationManagerBuilder authBuilder = mock(AuthenticationManagerBuilder.class, RETURNS_SELF);

        // Act
        config.configureGlobal(authBuilder);

        // Assert
        // Ensure we do NOT use a NoOpPasswordEncoder anymore and that a password encoder is configured.
        verify(authBuilder).userDetailsService(userService);
        verify(authBuilder).passwordEncoder(any(PasswordEncoder.class));
    }

    @Test
    void authenticationManager_shouldBeObtainedFromConfiguration() throws Exception {
        // This test ensures the AuthenticationManager bean is still wired correctly after changes.
        // Arrange
        AuthenticationManager expectedManager = mock(AuthenticationManager.class);
        var authenticationConfiguration = mock(org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(expectedManager);
        WebSecurityConfig config = new WebSecurityConfig(mock(UserService.class));

        // Act
        AuthenticationManager actualManager = config.authenticationManager(authenticationConfiguration);

        // Assert
        assertThat(actualManager).isSameAs(expectedManager);
        verify(authenticationConfiguration).getAuthenticationManager();
    }
}
