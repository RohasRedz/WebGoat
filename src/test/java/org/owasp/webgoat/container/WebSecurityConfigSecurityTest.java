package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.support.StaticWebApplicationContext;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import java.io.IOException;

/**
 * Delta unit tests for WebSecurityConfig focusing only on:
 * - Use of BCryptPasswordEncoder instead of NoOpPasswordEncoder (VULN-1 & VULN-3).
 * - CSRF being enabled (no longer disabled) (VULN-2).
 * - Security headers configuration (X-Frame-Options, X-Content-Type-Options, Referrer-Policy,
 *   Content-Security-Policy, Permissions-Policy).
 *
 * NOTE: These tests are intentionally targeted to the changed behavior and do not try to cover
 * unrelated aspects of the configuration.
 */
public class WebSecurityConfigSecurityTest {

  private WebSecurityConfig webSecurityConfig;
  private UserService userServiceMock;
  private AuthenticationConfiguration authenticationConfigurationMock;

  @BeforeEach
  void setUp() throws Exception {
    userServiceMock = Mockito.mock(UserService.class);
    authenticationConfigurationMock = Mockito.mock(AuthenticationConfiguration.class);
    AuthenticationManager authenticationManagerMock = Mockito.mock(AuthenticationManager.class);
    Mockito.when(authenticationConfigurationMock.getAuthenticationManager())
        .thenReturn(authenticationManagerMock);

    webSecurityConfig = new WebSecurityConfig(userServiceMock);
  }

  @Test
  void passwordEncoderBeanShouldBeBCryptPasswordEncoder() {
    // Arrange & Act
    PasswordEncoder encoder = webSecurityConfig.passwordEncoder();

    // Assert
    assertThat(encoder)
        .isNotNull()
        .isInstanceOf(BCryptPasswordEncoder.class);
  }

  @Test
  void userDetailsServiceBeanShouldExposeConfiguredUserService() {
    // Arrange & Act
    UserDetailsService userDetailsService = webSecurityConfig.userDetailsServiceBean();

    // Assert
    assertThat(userDetailsService).isSameAs(userServiceMock);
  }

  @Test
  void csrfShouldBeEnabledAndTokenExposedForProtectedEndpoint() throws Exception {
    // Arrange
    StaticWebApplicationContext context = new StaticWebApplicationContext();
    context.registerSingleton("webSecurityConfig", WebSecurityConfig.class);
    context.getBeanFactory().registerSingleton("userService", userServiceMock);
    context.refresh();

    SecurityFilterChain securityFilterChain =
        webSecurityConfig.filterChain(new org.springframework.security.config.annotation.web.builders.HttpSecurity(
                context.getBean(AuthenticationConfiguration.class),
                context.getBeanFactory(),
                org.springframework.security.config.Customizer.withDefaults()
        ));

    Filter springSecurityFilterChain = new FilterChainProxy(securityFilterChain);

    MockHttpServletRequest request =
        MockMvcRequestBuilders.get("/welcome.mvc").buildRequest(context.getServletContext());
    MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    springSecurityFilterChain.doFilter(request, response, (req, res) -> {});

    // Assert
    // CSRF filter should have created a token attribute
    Object csrfAttr = request.getAttribute(CsrfToken.class.getName());
    assertThat(csrfAttr)
        .as("CSRF token should be present when CSRF is enabled")
        .isInstanceOf(CsrfToken.class);
  }

  @Test
  void securityHeadersShouldBePresentOnResponse() throws ServletException, IOException {
    // Arrange
    StaticWebApplicationContext context = new StaticWebApplicationContext();
    context.registerSingleton("webSecurityConfig", WebSecurityConfig.class);
    context.getBeanFactory().registerSingleton("userService", userServiceMock);
    context.refresh();

    SecurityFilterChain securityFilterChain =
        webSecurityConfig.filterChain(new org.springframework.security.config.annotation.web.builders.HttpSecurity(
                context.getBean(AuthenticationConfiguration.class),
                context.getBeanFactory(),
                org.springframework.security.config.Customizer.withDefaults()
        ));

    Filter springSecurityFilterChain = new FilterChainProxy(securityFilterChain);

    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/welcome.mvc");
    MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    springSecurityFilterChain.doFilter(request, response, (req, res) -> {
      // Simulate normal controller processing without adding/overwriting headers
      ((MockHttpServletResponse) res).setStatus(HttpStatus.OK.value());
    });

    HttpHeaders headers = new HttpHeaders();
    response.getHeaderNames().forEach(
        name -> headers.add(name, response.getHeader(name))
    );

    // Assert: X-Frame-Options
    assertThat(headers.getFirst("X-Frame-Options"))
        .as("X-Frame-Options should be set to DENY")
        .isEqualToIgnoringCase("DENY");

    // Assert: X-Content-Type-Options
    assertThat(headers.getFirst("X-Content-Type-Options"))
        .as("X-Content-Type-Options should be set to nosniff")
        .isEqualToIgnoringCase("nosniff");

    // Assert: Referrer-Policy
    assertThat(headers.getFirst("Referrer-Policy"))
        .as("Referrer-Policy should be strict-origin-when-cross-origin")
        .isEqualToIgnoringCase("strict-origin-when-cross-origin");

    // Assert: Content-Security-Policy
    assertThat(headers.getFirst("Content-Security-Policy"))
        .as("Content-Security-Policy should contain the configured basic CSP")
        .contains("default-src 'self'")
        .contains("script-src 'self' 'unsafe-inline'")
        .contains("style-src 'self' 'unsafe-inline'");

    // Assert: Permissions-Policy (or newer header name where applicable)
    String permissionsPolicy =
        headers.getFirst("Permissions-Policy") != null
            ? headers.getFirst("Permissions-Policy")
            : headers.getFirst("Feature-Policy"); // fallback for older containers, if any

    assertThat(permissionsPolicy)
        .as("Permissions-Policy (or Feature-Policy) should restrict camera and microphone")
        .isNotNull()
        .contains("camera=()")
        .contains("microphone=()");
  }
}
