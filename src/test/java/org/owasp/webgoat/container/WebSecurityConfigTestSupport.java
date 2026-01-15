package org.owasp.webgoat.container;

import org.owasp.webgoat.container.users.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

/**
 * Minimal support configuration to bootstrap WebSecurityConfig for unit testing
 * the PasswordEncoder bean without bringing up the full application.
 */
@Configuration
public class WebSecurityConfigTestSupport extends WebSecurityConfig {

  public WebSecurityConfigTestSupport() {
    super(new UserService(null, null, null));
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return super.authenticationManager(authenticationConfiguration);
  }
}
