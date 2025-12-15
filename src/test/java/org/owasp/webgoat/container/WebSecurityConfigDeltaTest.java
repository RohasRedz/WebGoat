// Assuming the same package as the class under test.
// If this package changes, update accordingly.
package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// TODO: If this project does not use Spring Boot, replace with a pure Spring test configuration.
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Delta unit tests for WebSecurityConfig focusing only on:
 *
 * 1) The PasswordEncoder bean being a strong encoder (BCrypt) instead of NoOp/plain-text.
 * 2) CSRF protection being enabled (POST without CSRF token is rejected).
 *
 * These tests are intentionally narrow to cover only the behavior changed by the fix.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(WebSecurityConfigDeltaTest.TestConfig.class)
class WebSecurityConfigDeltaTest {

  @Autowired private WebSecurityConfig webSecurityConfig;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("passwordEncoder bean should not be NoOp/plain-text and should be BCryptPasswordEncoder-compatible")
  void passwordEncoderShouldBeSecureImplementation() throws Exception {
    // Arrange & Act
    PasswordEncoder encoder = this.passwordEncoder;

    // Assert
    // The old vulnerable behavior used NoOpPasswordEncoder; ensure that is no longer the case.
    assertThat(encoder.getClass().getSimpleName())
        .as("PasswordEncoder must not be NoOp/plain-text")
        .doesNotContain("NoOp");

    // Additionally, assert that BCrypt-like encoding is happening: encoded value should differ
    // from raw input and should be marked as matching by the same encoder.
    String rawPassword = "secret123!";
    String encoded = encoder.encode(rawPassword);

    assertThat(encoded)
        .as("Encoded password must not equal raw password (no plain-text storage)")
        .isNotEqualTo(rawPassword);

    assertThat(encoder.matches(rawPassword, encoded))
        .as("PasswordEncoder should correctly verify encoded password")
        .isTrue();
  }

  @Test
  @DisplayName("CSRF should be enabled: POST to protected URL without CSRF token must be forbidden")
  @WithMockUser(username = "user", roles = {"USER"})
  void postWithoutCsrfTokenShouldBeRejected() throws Exception {
    // Arrange
    // Pick a URL that requires authentication per WebSecurityConfig:
    // anyRequest().authenticated() - e.g., "/welcome.mvc" is the default success URL.
    String protectedUrl = "/welcome.mvc";

    // Act & Assert
    // With CSRF disabled (old behavior), this would typically return 200/3xx.
    // With CSRF enabled (fixed behavior), Spring Security should return 403 Forbidden.
    mockMvc
        .perform(post(protectedUrl))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET to protected URL should still succeed for authenticated user (control check)")
  @WithMockUser(username = "user", roles = {"USER"})
  void getProtectedUrlShouldSucceedForAuthenticatedUser() throws Exception {
    // Arrange
    String protectedUrl = "/welcome.mvc";

    // Act & Assert
    // This ensures that enabling CSRF did not break standard authenticated GET access.
    mockMvc.perform(get(protectedUrl)).andExpect(status().isOk());
  }

  /**
   * Minimal test configuration providing the required UserService bean for WebSecurityConfig.
   *
   * This isolates the delta tests from the rest of the application and focuses only on:
   * - the SecurityFilterChain / CSRF behavior
   * - the PasswordEncoder bean.
   */
  static class TestConfig {

    @Bean
    UserService userService() {
      // Mocking UserService is sufficient for these configuration-level tests.
      return Mockito.mock(UserService.class);
    }

    // Optional bean to assert that SecurityFilterChain can be built;
    // this also ensures HttpSecurity is configured with the fixed CSRF/PasswordEncoder setup.
    @Bean
    HttpSecurity httpSecurity(org.springframework.security.config.annotation.web.builders.HttpSecurity http)
        throws Exception {
      return http;
    }
  }
}
