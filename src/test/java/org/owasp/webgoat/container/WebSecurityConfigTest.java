package org.owasp.webgoat.container;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Delta tests for WebSecurityConfig focusing on:
 *  - replacement of NoOpPasswordEncoder with BCryptPasswordEncoder
 */
public class WebSecurityConfigTest {

  @Test
  void passwordEncoder_isBCryptAndNotNoOp() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(WebSecurityConfigTestSupport.class);
    ctx.refresh();

    PasswordEncoder encoder = ctx.getBean(PasswordEncoder.class);

    assertNotNull(encoder);
    assertTrue(encoder instanceof BCryptPasswordEncoder, "PasswordEncoder should be BCryptPasswordEncoder");

    String raw = "secretPassword";
    String hash = encoder.encode(raw);
    assertNotEquals(raw, hash, "Encoded password should not equal raw password");
    assertTrue(encoder.matches(raw, hash), "Encoder should validate its own hashes");

    ctx.close();
  }
}
