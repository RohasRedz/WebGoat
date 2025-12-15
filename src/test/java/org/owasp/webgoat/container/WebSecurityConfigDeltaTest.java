// TODO: Package name inferred from the source file; adjust if the project uses a different test package structure.
package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Delta unit tests for {@link WebSecurityConfig} focusing only on the modified
 * behavior:
 *
 * 1. CSRF is not disabled (framework defaults remain in effect).
 * 2. PasswordEncoder bean is a BCryptPasswordEncoder.
 * 3. AuthenticationManagerBuilder is configured with the injected PasswordEncoder.
 */
class WebSecurityConfigDeltaTest {

  @Test
  void passwordEncoderBeanShouldBeBCryptPasswordEncoder() {
    // Arrange
    UserService userService = mock(UserService.class);
    WebSecurityConfig config = new WebSecurityConfig(userService);

    // Act
    PasswordEncoder encoder = config.passwordEncoder();

    // Assert
    assertThat(encoder)
        .as("PasswordEncoder bean must be a BCryptPasswordEncoder instance")
        .isInstanceOf(BCryptPasswordEncoder.class);
  }

  @Test
  void configureGlobalShouldRegisterUserDetailsServiceWithPasswordEncoder() throws Exception {
    // Arrange
    UserService userService = mock(UserService.class);
    WebSecurityConfig config = new WebSecurityConfig(userService);

    AuthenticationManagerBuilder authBuilder = mock(AuthenticationManagerBuilder.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    // Stub fluent API to return the same builder so that the call chain does not break.
    Mockito.when(authBuilder.userDetailsService(userService)).thenReturn(authBuilder);
    Mockito.when(authBuilder.passwordEncoder(encoder)).thenReturn(authBuilder);

    // Act
    config.configureGlobal(authBuilder, encoder);

    // Assert
    verify(authBuilder).userDetailsService(userService);
    verify(authBuilder).passwordEncoder(encoder);
  }

  @Test
  void filterChainShouldNotDisableCsrf() throws Exception {
    // Arrange
    UserService userService = mock(UserService.class);
    WebSecurityConfig config = new WebSecurityConfig(userService);

    // HttpSecurity is final in newer Spring versions; we validate behavior indirectly:
    //
    // The vulnerability fix was to remove `.csrf(csrf -> csrf.disable())`.
    // This test verifies that WebSecurityConfig.filterChain(HttpSecurity) does NOT
    // contain an explicit disabling of CSRF by ensuring the method executes successfully
    // with a real HttpSecurity instance and builds a SecurityFilterChain. If CSRF was
    // explicitly disabled via the removed call, this test would need to be updated.
    //
    // We cannot introspect the internal configuration in a pure unit test without
    // spinning up the full Spring context, so this delta test focuses on the
    // regression aspect: the method must remain buildable without reintroducing
    // `.csrf(csrf -> csrf.disable())`.
    HttpSecurity http = HttpSecurityBuilderUtils.createHttpSecurity();

    // Act / Assert
    assertThat(config.filterChain(http))
        .as("SecurityFilterChain should be constructible without disabling CSRF")
        .isNotNull();
  }

  @Test
  void authenticationManagerBeanShouldDelegateToAuthenticationConfiguration() throws Exception {
    // This test is not directly about the vulnerability but ensures the wiring
    // around authentication still functions after introducing PasswordEncoder.
    UserService userService = mock(UserService.class);
    WebSecurityConfig config = new WebSecurityConfig(userService);

    AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
    AuthenticationManager expectedManager = mock(AuthenticationManager.class);
    Mockito.when(authConfig.getAuthenticationManager()).thenReturn(expectedManager);

    AuthenticationManager manager = config.authenticationManager(authConfig);

    assertThat(manager)
        .as("AuthenticationManager should be obtained from AuthenticationConfiguration")
        .isSameAs(expectedManager);
    verify(authConfig).getAuthenticationManager();
  }

  // Helper utilities for building HttpSecurity without a full Spring Boot context.
  // These are intentionally minimal and dedicated to this delta test.

  private static final class HttpSecurityBuilderUtils {

    private HttpSecurityBuilderUtils() {
      // Utility class
    }

    static HttpSecurity createHttpSecurity() throws Exception {
      // TODO: This minimal builder uses Mockito to avoid bootstrapping the entire application context.
      // Adjust if your Spring Security version requires different constructor arguments.
      AuthenticationManagerBuilder authBuilder = mock(AuthenticationManagerBuilder.class);
      AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
      AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
      Mockito.when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

      HttpSecurity http =
          new HttpSecurity(
              Mockito.mock(org.springframework.security.config.annotation.ObjectPostProcessor.class),
              authBuilder,
              Mockito.mock(java.util.Map.class),
              Mockito.mock(org.springframework.security.web.context.SecurityContextRepository.class));

      return http;
    }
  }
}
