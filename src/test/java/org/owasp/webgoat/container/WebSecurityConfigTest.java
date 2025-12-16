// TODO: Package inferred from source path; adjust if actual package differs.
package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.csrf.CsrfToken;

/**
 * Delta tests for WebSecurityConfig focusing only on changed behavior:
 * - CSRF is enabled (no longer disabled)
 * - PasswordEncoder is a BCrypt-based, non-NoOp encoder and is wired into AuthenticationManagerBuilder
 */
public class WebSecurityConfigTest {

    @Test
    void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert: verify we are no longer using NoOp/plain-text and that BCrypt works
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        String raw = "secret123";
        String hashed = encoder.encode(raw);
        assertThat(hashed).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, hashed)).isTrue();
    }

    @Test
    void configureGlobal_ShouldConfigureUserDetailsServiceWithPasswordEncoder() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        AuthenticationManagerBuilder builder =
                Mockito.mock(AuthenticationManagerBuilder.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(builder.userDetailsService(userService)).thenReturn(builder);

        // Act
        config.configureGlobal(builder);

        // Assert: ensure passwordEncoder is used when configuring auth
        Mockito.verify(builder).userDetailsService(userService);
        Mockito.verify(builder).passwordEncoder(Mockito.any(PasswordEncoder.class));
    }

    @Test
    void filterChain_ShouldHaveCsrfProtectionEnabledByDefault() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // NOTE: Creating HttpSecurity directly is simplified and may need adaptation in real tests.
        HttpSecurity httpSecurity = new HttpSecurity(null, null, null, null, null, null, null);

        // Act
        SecurityFilterChain chain = config.filterChain(httpSecurity);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/some/protected/url");
        chain.matches(request);

        // Assert: CSRF token attribute should be present when CSRF is enabled
        Object csrfAttr = request.getAttribute(CsrfToken.class.getName());
        assertThat(csrfAttr)
                .as("CSRF token attribute should be present when CSRF is enabled")
                .isNotNull();
    }

    @Test
    void userDetailsServiceBean_ShouldReturnInjectedUserService() {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        // Act
        UserDetailsService uds = config.userDetailsServiceBean();

        // Assert
        assertThat(uds).isSameAs(userService);
    }

    @Test
    void authenticationManager_ShouldDelegateToAuthenticationConfiguration() throws Exception {
        // Arrange
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);

        AuthenticationManager expected = mock(AuthenticationManager.class);
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        Mockito.when(authenticationConfiguration.getAuthenticationManager()).thenReturn(expected);

        // Act
        AuthenticationManager result = config.authenticationManager(authenticationConfiguration);

        // Assert
        assertThat(result).isSameAs(expected);
        Mockito.verify(authenticationConfiguration).getAuthenticationManager();
    }
}
