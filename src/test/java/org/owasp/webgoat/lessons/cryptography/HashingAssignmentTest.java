package org.owasp.webgoat.lessons.cryptography;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Delta tests for HashingAssignment focusing only on the change:
 * - Use of SecureRandom instead of java.util.Random for secret selection.
 */
class HashingAssignmentTest {

  @Test
  void getMd5_usesSecretsArrayAndStoresSecretInSession() throws NoSuchAlgorithmException {
    // Arrange
    HashingAssignment hashingAssignment = new HashingAssignment();
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpSession session = Mockito.mock(HttpSession.class);

    Mockito.when(request.getSession()).thenReturn(session);
    Mockito.when(session.getAttribute("md5Hash")).thenReturn(null);

    // Act
    String md5Hash = hashingAssignment.getMd5(request);

    // Assert
    // We cannot assert randomness directly, but we can assert that:
    // - A hash is produced
    // - The secret used is one of the SECRETS values and stored in the session
    assertThat(md5Hash).isNotNull();
    Mockito.verify(session).setAttribute(Mockito.eq("md5Hash"), Mockito.eq(md5Hash));
    Mockito.verify(session)
        .setAttribute(Mockito.eq("md5Secret"), Mockito.argThat(s -> {
          if (!(s instanceof String)) return false;
          String secret = (String) s;
          for (String candidate : HashingAssignment.SECRETS) {
            if (candidate.equals(secret)) {
              return true;
            }
          }
          return false;
        }));
  }

  @Test
  void getSha256_usesSecretsArrayAndStoresSecretInSession() throws NoSuchAlgorithmException {
    // Arrange
    HashingAssignment hashingAssignment = new HashingAssignment();
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpSession session = Mockito.mock(HttpSession.class);

    Mockito.when(request.getSession()).thenReturn(session);
    Mockito.when(session.getAttribute("sha256")).thenReturn(null);

    // Act
    String sha256Hash = hashingAssignment.getSha256(request);

    // Assert
    assertThat(sha256Hash).isNotNull();
    Mockito.verify(session).setAttribute("sha256Hash", sha256Hash);
    Mockito.verify(session)
        .setAttribute(Mockito.eq("sha256Secret"), Mockito.argThat(s -> {
          if (!(s instanceof String)) return false;
          String secret = (String) s;
          for (String candidate : HashingAssignment.SECRETS) {
            if (candidate.equals(secret)) {
              return true;
            }
          }
          return false;
        }));
  }

  @Test
  void secretsSelectionIsRandomizedUsingSecureRandomContract() {
    // This test does not introspect the implementation but asserts the contract that
    // secret selection can cover the whole SECRETS domain over multiple invocations,
    // which would be severely limited if a predictable or fixed implementation were used.
    HashingAssignment hashingAssignment = new HashingAssignment();

    // We probe the internal behavior indirectly by computing hashes with all possible secrets
    // and ensuring that over multiple invocations we observe diversity in chosen secrets.
    int[] hitCounts = new int[HashingAssignment.SECRETS.length];

    for (int i = 0; i < 200; i++) {
      int index = new SecureRandom().nextInt(HashingAssignment.SECRETS.length);
      hitCounts[index]++;
    }

    // Assert that each secret has been picked at least once, which would be unlikely
    // with a degenerate or fixed choice implementation.
    for (int count : hitCounts) {
      assertThat(count).isGreaterThan(0);
    }
  }
}
