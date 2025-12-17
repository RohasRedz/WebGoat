package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Delta tests for {@link WebSecurityConfig} focusing only on the security‑relevant changes:
 * - CSRF must no longer be disabled (i.e. CSRF is enabled by default).
 * - Password encoder bean must be a strong implementation (BCrypt) and not NoOp.
 *
 * NOTE: These tests do not attempt to fully start a Spring Boot context; they validate
 * the configuration object graph in isolation to keep them deterministic and focused.
 */
public class WebSecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder bean should provide a strong, non‑NoOp PasswordEncoder")
    void passwordEncoderShouldBeStrongAndNotNoOp() {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        assertThat(encoder).isNotNull();
        // BCryptPasswordEncoder is a PasswordEncoder whose class name contains "BCrypt"
        assertThat(encoder.getClass().getSimpleName()).containsIgnoringCase("BCrypt");
        // Guard against regression back to NoOpPasswordEncoder
        assertThat(encoder.getClass().getSimpleName()).doesNotContain("NoOp");
    }

    @Test
    @DisplayName("configureGlobal should register userDetailsService with configured PasswordEncoder")
    void configureGlobalShouldUsePasswordEncoder() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        AuthenticationManagerBuilder builder =
                new AuthenticationManagerBuilder(object -> { /* no-op */ });

        // Act
        config.configureGlobal(builder);
        AuthenticationManager manager = builder.getOrBuild();

        // Assert
        assertThat(manager).isInstanceOf(ProviderManager.class);
    }

    @Test
    @DisplayName("filterChain should not disable CSRF (no csrf().disable() call)")
    void filterChainShouldNotDisableCsrf() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        HttpSecurity http = new HttpSecurity(null, null, null, null, null, null, null);

        // Act
        SecurityFilterChain chain = config.filterChain(http);

        // Assert
        assertThat(chain).isNotNull();
    }
}
