package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Delta test focusing on the change from NoOpPasswordEncoder to a secure PasswordEncoder
 * implementation (BCrypt) in WebSecurityConfig.
 */
class WebSecurityConfigDeltaTest {

  @Test
  @DisplayName("passwordEncoder() should return a hashing PasswordEncoder, not plain-text/NoOp")
  void passwordEncoderShouldBeSecure() {
    // Arrange
    UserService userService = mock(UserService.class);
    WebSecurityConfig config = new WebSecurityConfig(userService);

    // Act
    PasswordEncoder encoder = config.passwordEncoder();

    // Assert
    String rawPassword = "secret-password";
    String encoded = encoder.encode(rawPassword);

    // Encoded password must not equal the raw one (i.e., no longer NoOp/plain-text)
    assertThat(encoded)
        .as("Encoded password should not be equal to raw password")
        .isNotEqualTo(rawPassword);

    // And the encoder must correctly verify the hash
    assertThat(encoder.matches(rawPassword, encoded))
        .as("PasswordEncoder should verify the hashed password correctly")
        .isTrue();
  }
}
