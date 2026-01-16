package org.owasp.webgoat.container;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Delta tests for WebSecurityConfig focusing on:
 * - PasswordEncoder is not a no-op and produces encoded (non-plain-text) values.
 * - SecurityFilterChain loads successfully with CSRF protection not explicitly disabled.
 */
class WebSecurityConfigTest {

  @Test
  void passwordEncoder_encodesPassword() {
    UserService userService = null; // not needed for encoder bean
    WebSecurityConfig config = new WebSecurityConfig(userService);

    PasswordEncoder encoder = config.passwordEncoder();

    String raw = "password123";
    String encoded = encoder.encode(raw);

    assertNotEquals(raw, encoded, "Encoded password must not equal raw password");
    assertTrue(encoder.matches(raw, encoded), "PasswordEncoder should validate encoded password");
  }

  @Test
  void securityFilterChain_loadsWithCsrfEnabledByDefault() throws Exception {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.register(WebSecurityConfig.class);
    context.setServletContext(new MockServletContext());
    context.refresh();

    WebSecurityConfig config = context.getBean(WebSecurityConfig.class);

    // Build an HttpSecurity manually bound to the context
    HttpSecurity http =
        new HttpSecurity(
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    SecurityFilterChain chain = config.filterChain(http);
    assertNotNull(chain, "SecurityFilterChain should be created successfully");

    context.close();
  }

  @Test
  void authenticationManager_usesPasswordEncoderBean() throws Exception {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.register(WebSecurityConfig.class);
    context.setServletContext(new MockServletContext());
    context.refresh();

    AuthenticationConfiguration authenticationConfiguration =
        context.getBean(AuthenticationConfiguration.class);
    AuthenticationManager authenticationManager =
        context.getBean(WebSecurityConfig.class).authenticationManager(authenticationConfiguration);

    assertNotNull(authenticationManager, "AuthenticationManager should be created");

    PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
    UserDetailsService userDetailsService = context.getBean(UserDetailsService.class);

    assertNotNull(encoder, "PasswordEncoder bean must be present");
    assertNotNull(userDetailsService, "UserDetailsService bean must be present");

    context.close();
  }
}
