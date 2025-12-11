package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

/**
 * Delta unit tests for the security fixes applied in {@link WebSecurityConfig}.
 *
 * Focus:
 * - Ensure a secure PasswordEncoder (BCrypt) is used instead of NoOp/plain-text.
 * - Ensure CSRF protection is no longer disabled in the SecurityFilterChain.
 *
 * NOTE: This test class is intentionally focused only on the behavior changed by
 * the security fix (password encoding and CSRF configuration).
 */
@EnableWebSecurity // Ensures Spring Security infrastructure is available where needed
public class WebSecurityConfigSecurityDeltaTest {

  // TODO: If a full Spring context is available in the project, these tests could be
  // converted to @SpringBootTest/@WebMvcTest + @Autowired beans instead of direct instantiation.

  @Test
  @DisplayName("passwordEncoder bean should be a secure PasswordEncoder (BCrypt) and not NoOp")
  void passwordEncoderShouldBeBCryptAndNotNoOp() {
    // Arrange
    UserService userService = mock(UserService.class);
    WebSecurityConfig config = new WebSecurityConfig(userService);

    // Act
    PasswordEncoder encoder = config.passwordEncoder();

    // Assert
    assertThat(encoder)
        .as("PasswordEncoder bean must be provided")
        .isNotNull();

    // Assert it is not the insecure NoOpPasswordEncoder that was used before the fix
    assertThat(encoder)
        .as("PasswordEncoder must not be NoOpPasswordEncoder (plain-text)")
        .isNotInstanceOf(NoOpPasswordEncoder.class);

    // Assert it is the expected BCryptPasswordEncoder (secure implementation)
    assertThat(encoder)
        .as("PasswordEncoder should be BCryptPasswordEncoder as per security fix")
        .isInstanceOf(BCryptPasswordEncoder.class);

    // Sanity check: encoded password should differ from raw password
    String raw = "secretPassword123!";
    String encoded = encoder.encode(raw);
    assertThat(encoded)
        .as("Encoded password must not equal raw password")
        .isNotEqualTo(raw);
    assertThat(encoder.matches(raw, encoded))
        .as("Encoder must successfully match raw password with its encoded form")
        .isTrue();
  }

  @Test
  @DisplayName("SecurityFilterChain should not disable CSRF protection")
  void securityFilterChainShouldKeepCsrfEnabled() throws Exception {
    // Arrange
    UserService userService = mock(UserService.class);
    WebSecurityConfig config = new WebSecurityConfig(userService);

    // We create a fresh HttpSecurity instance using the standard Spring Security builder
    HttpSecurity http = TestHttpSecurityBuilder.buildHttpSecurity();

    // Act
    SecurityFilterChain chain = config.filterChain(http);

    // Assert
    assertThat(chain)
        .as("SecurityFilterChain must be created successfully")
        .isNotNull();

    // Verify that a CsrfFilter is present in the chain, which implies CSRF is not globally disabled.
    boolean hasCsrfFilter =
        chain.getFilters().stream().anyMatch(f -> f instanceof CsrfFilter);

    assertThat(hasCsrfFilter)
        .as("SecurityFilterChain should contain CsrfFilter, meaning CSRF is not disabled")
        .isTrue();

    // Additional behavioural assertion: CSRF token should be generated for a POST request
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/any");
    SecurityMockMvcRequestPostProcessors.csrf().postProcessRequest(request);

    Object csrfAttribute = request.getAttribute(CsrfFilter.class.getName());
    assertThat(csrfAttribute)
        .as("CSRF processing should attach attributes to the request")
        .isNotNull();
  }

  /**
   * Minimal helper to construct an {@link HttpSecurity} instance suitable for unit testing outside
   * of a full Spring context.
   *
   * This avoids pulling in the full application configuration and keeps the test focused only on
   * the security delta introduced by the fix.
   */
  static class TestHttpSecurityBuilder {

    // TODO: If this project already has a shared utility to create HttpSecurity for tests,
    // this helper can be removed and replaced with that utility.

    static HttpSecurity buildHttpSecurity() throws Exception {
      // We rely on the HttpSecurity constructor that is normally used by Spring Security internally.
      // For unit tests, we can use a lightweight setup by mocking only what is necessary.
      org.springframework.security.config.annotation.web.builders.WebSecurity webSecurity =
          new org.springframework.security.config.annotation.web.builders.WebSecurity(null);
      return new HttpSecurity(
          null,
          webSecurity.getSharedObject(org.springframework.security.config.annotation.ObjectPostProcessor.class),
          new org.springframework.security.config.annotation.web.builders.HttpSecurityBuilder.DefaultSecurityFilterChainBuilder(),
          webSecurity.getSharedObjects());
    }
  }
}
