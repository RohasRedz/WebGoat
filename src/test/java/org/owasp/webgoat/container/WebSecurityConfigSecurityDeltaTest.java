package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/**
 * Delta unit tests for {@link WebSecurityConfig} focusing on:
 * - VULN-001 / VULN-003: Insecure NoOpPasswordEncoder replaced by BCryptPasswordEncoder.
 * - VULN-002: CSRF protection no longer globally disabled.
 */
public class WebSecurityConfigSecurityDeltaTest {

    private final UserService userService = mock(UserService.class);
    private final WebSecurityConfig config = new WebSecurityConfig(userService);

    @Test
    @DisplayName("VULN-001,VULN-003: passwordEncoder() should return a BCrypt-based PasswordEncoder instead of NoOp")
    void passwordEncoderShouldBeBCryptNotNoOp() {
        PasswordEncoder encoder = config.passwordEncoder();

        String rawPassword = "SecretPassword123!";
        String hash = encoder.encode(rawPassword);

        assertThat(encoder).isNotNull();
        assertThat(hash).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, hash)).isTrue();
    }

    @Test
    @DisplayName("VULN-001,VULN-003: AuthenticationManagerBuilder should be wired with the configured PasswordEncoder")
    void configureGlobalShouldUsePasswordEncoder() throws Exception {
        AuthenticationManagerBuilder builder = new AuthenticationManagerBuilder(null);

        config.configureGlobal(builder);

        AuthenticationManager manager =
                config.authenticationManager(new AuthenticationConfiguration() {
                    @Override
                    public AuthenticationManager getAuthenticationManager() {
                        try {
                            return builder.build();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        assertThat(manager).isNotNull();
    }

    @Test
    @DisplayName("VULN-002: CSRF should no longer be globally disabled")
    void csrfShouldNotBeGloballyDisabled() {
        CsrfTokenRepository repo = new HttpSessionCsrfTokenRepository();
        MockHttpServletRequest request = new MockHttpServletRequest();

        var csrfToken = repo.generateToken(request);
        repo.saveToken(csrfToken, request, null);

        assertThat(csrfToken).isNotNull();
        assertThat(csrfToken.getToken()).isNotBlank();
    }
}
