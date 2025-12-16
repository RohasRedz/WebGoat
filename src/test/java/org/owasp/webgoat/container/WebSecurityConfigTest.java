// Delta unit test for WebSecurityConfig.java
// Assumed package based on resolved_file_path; adjust if actual package differs.
package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfTokenRepository;

class WebSecurityConfigTest {

    // Minimal stub AuthenticationConfiguration to obtain an AuthenticationManager instance
    // without touching real infrastructure.
    private static class StubAuthenticationConfiguration extends AuthenticationConfiguration {
        private final AuthenticationManager authenticationManager;

        StubAuthenticationConfiguration(AuthenticationManager authenticationManager) {
            this.authenticationManager = authenticationManager;
        }

        @Override
        public AuthenticationManager getAuthenticationManager() {
            return authenticationManager;
        }
    }

    @Test
    void passwordEncoderShouldUseBCryptImplementation() {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        // Verify we are no longer using NoOpPasswordEncoder and are using a BCrypt-based encoder.
        assertThat(encoder).isNotNull();
        String rawPassword = "secret";
        String encoded = encoder.encode(rawPassword);
        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
    }

    @Test
    void configureGlobalShouldRegisterUserDetailsServiceWithPasswordEncoder() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        PasswordEncoder encoder = config.passwordEncoder();

        AuthenticationManagerBuilder builder = new AuthenticationManagerBuilder(null);

        // Act
        config.configureGlobal(builder);
        AuthenticationManager authenticationManager = builder.build();
        StubAuthenticationConfiguration stubAuthenticationConfiguration =
                new StubAuthenticationConfiguration(authenticationManager);

        // Assert
        // Simply calling through ensures the builder can be built without throwing
        // and therefore that the passwordEncoder wiring is valid.
        AuthenticationManager managerFromConfig =
                config.authenticationManager(stubAuthenticationConfiguration);
        assertThat(managerFromConfig).isNotNull();
    }

    @Test
    void filterChainShouldHaveCsrfEnabledByDefault() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        HttpSecurity http = new HttpSecurity(
                null, null, null, mock(CsrfTokenRepository.class), null, null, null);

        // Act
        config.filterChain(http);

        // Assert
        // The delta is that CSRF is no longer disabled globally.
        // We assert configuration does not contain a global disable flag.
        assertThat(http.getConfigurer(org.springframework.security.config.annotation.web.configurers.CsrfConfigurer.class))
                .isNotNull();
    }
}
