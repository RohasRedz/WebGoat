package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Delta test verifying that CSRF protection is enabled in the security filter chain
 * (previously it was explicitly disabled with csrf().disable()).
 */
class WebSecurityConfigCsrfDeltaTest {

  @Test
  @DisplayName("filterChain should include CSRF filter, indicating CSRF is enabled")
  void filterChainShouldHaveCsrfEnabled() throws Exception {
    // Arrange
    UserService userService = mock(UserService.class);
    WebSecurityConfig config = new WebSecurityConfig(userService);

    AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    org.mockito.Mockito.when(authenticationConfiguration.getAuthenticationManager())
        .thenReturn(authenticationManager);

    HttpSecurity http =
        new HttpSecurity(
            mock(ObjectPostProcessor.class), authenticationConfiguration, Collections.emptyMap());

    // Act
    SecurityFilterChain chain = config.filterChain(http);

    // Assert
    boolean hasCsrfFilter =
        chain.getFilters().stream()
            .anyMatch(f -> f.getClass().getName().contains("CsrfFilter"));

    assertThat(hasCsrfFilter)
        .as("CSRF filter must be present, meaning CSRF protection is not disabled")
        .isTrue();
  }
}
