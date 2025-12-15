package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Delta tests for the security fix in WebSecurityConfig:
 * - Verifies that a strong PasswordEncoder is used instead of NoOpPasswordEncoder.
 * - Verifies that the PasswordEncoder is wired into AuthenticationManagerBuilder.
 *
 * These tests focus ONLY on the changed behavior related to password encoding.
 */
public class WebSecurityConfigDeltaTest {

    @Test
    void passwordEncoderShouldHashPasswordsAndNotBeNoOp() {
        // Arrange
        UserService userService = Mockito.mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "SensitivePassword!123";
        String encodedPassword = encoder.encode(rawPassword);

        // Assert
        // Previously vulnerable behavior: NoOpPasswordEncoder would have returned rawPassword.
        assertThat(encodedPassword)
                .as("Encoded password must not equal raw password (NoOp behavior removed)")
                .isNotEqualTo(rawPassword);

        // Secure behavior: encoder must correctly verify the encoded password.
        assertThat(encoder.matches(rawPassword, encodedPassword))
                .as("PasswordEncoder must successfully verify the encoded password")
                .isTrue();
    }

    @Test
    void configureGlobalMustRegisterPasswordEncoderWithAuthenticationManagerBuilder() throws Exception {
        // Arrange
        UserDetailsService inMemory =
                new InMemoryUserDetailsManager(
                        User.withUsername("user")
                                .password("dummy")
                                .roles("USER")
                                .build());

        UserService userService = Mockito.mock(UserService.class);
        Mockito.when(userService.loadUserByUsername(Mockito.anyString()))
                .thenAnswer(invocation -> inMemory.loadUserByUsername(invocation.getArgument(0)));

        WebSecurityConfig config = new WebSecurityConfig(userService);
        AuthenticationManager parentAuthManager = Mockito.mock(AuthenticationManager.class);
        AuthenticationManagerBuilder builder = new AuthenticationManagerBuilder(parentAuthManager);

        // Act
        config.configureGlobal(builder);

        // Assert
        Object encoderFromBuilder =
                ReflectionTestUtils.getField(builder, "defaultPasswordEncoderForMatches");

        assertThat(encoderFromBuilder)
                .as("AuthenticationManagerBuilder should be configured with a PasswordEncoder")
                .isInstanceOf(PasswordEncoder.class);
    }

    @Test
    void authenticationManagerBeanShouldDelegateToAuthenticationConfiguration() throws Exception {
        // Arrange
        UserService userService = Mockito.mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        AuthenticationManager expectedManager = Mockito.mock(AuthenticationManager.class);
        AuthenticationConfiguration authenticationConfiguration =
                Mockito.mock(AuthenticationConfiguration.class);
        Mockito.when(authenticationConfiguration.getAuthenticationManager())
                .thenReturn(expectedManager);

        // Act
        AuthenticationManager actualManager =
                config.authenticationManager(authenticationConfiguration);

        // Assert
        assertThat(actualManager)
                .as("authenticationManager bean must be obtained from AuthenticationConfiguration")
                .isSameAs(expectedManager);
    }
}
