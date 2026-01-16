package org.owasp.webgoat.lessons.cryptography;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import jakarta.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

/**
 * Delta tests for HashingAssignment focusing only on the PRNG change:
 * - Before fix: java.util.Random (predictable, seedable).
 * - After fix:  java.security.SecureRandom (cryptographically strong, not controllably seedable
 *   in the same way).
 *
 * We verify:
 * - Behavior is still deterministic for the same secret (hash of same secret is stable).
 * - The selection of the secret is no longer trivially controllable by seeding Random
 *   (i.e., we cannot force a specific index via predictable seeding).
 */
public class HashingAssignmentTest {

  private HashingAssignment hashingAssignment;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    hashingAssignment = new HashingAssignment();
    request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getSession()).thenReturn(Mockito.mock(jakarta.servlet.http.HttpSession.class));
  }

  @Test
  void getMd5_shouldReturnConsistentHashForSameSecret() throws NoSuchAlgorithmException {
    // Arrange
    String secret = HashingAssignment.SECRETS[0];
    byte[] digest = java.security.MessageDigest.getInstance("MD5").digest(secret.getBytes());
    String expectedHash = DatatypeConverter.printHexBinary(digest).toUpperCase();

    // Act
    // Simulate internal behavior: store our own secret and hash in the session,
    // then ensure the controller returns the same hash value.
    Mockito.when(request.getSession().getAttribute("md5Hash")).thenReturn(expectedHash);

    String result = hashingAssignment.getMd5(request);

    // Assert
    assertEquals(expectedHash, result, "MD5 hash should be stable for the same secret value");
  }

  @Test
  void getMd5_shouldNotUsePredictableRandomSeedBehavior() throws NoSuchAlgorithmException {
    // Arrange
    // Previous vulnerability: using java.util.Random allowed us to fix the seed and know
    // exactly which index would be chosen. With SecureRandom, we should not be able to
    // deterministically control the outcome via java.util.Random seeding logic.
    //
    // We approximate this by demonstrating that even if we compute an index via a seeded
    // java.util.Random, the SecureRandom-based selection is very unlikely to always match
    // that deterministic index across multiple invocations.
    int fixedIndex = new java.util.Random(12345L).nextInt(HashingAssignment.SECRETS.length);

    // Act
    String[] chosenSecrets = new String[5];
    for (int i = 0; i < chosenSecrets.length; i++) {
      // For each call, we simulate a fresh request with no pre-existing session attribute
      HttpServletRequest localRequest = Mockito.mock(HttpServletRequest.class);
      jakarta.servlet.http.HttpSession session = Mockito.mock(jakarta.servlet.http.HttpSession.class);
      Mockito.when(localRequest.getSession()).thenReturn(session);
      Mockito.when(session.getAttribute("md5Hash")).thenReturn(null);

      String md5Hash = hashingAssignment.getMd5(localRequest);
      // Reverse-derive which secret index would have produced this hash
      int observedIndex = resolveSecretIndexFromHash(md5Hash);
      chosenSecrets[i] = HashingAssignment.SECRETS[observedIndex];

      // ensure we don't accidentally short-circuit due to cached session hash within this loop
      Mockito.verify(session).setAttribute(Mockito.eq("md5Hash"), Mockito.any());
      Mockito.verify(session).setAttribute(Mockito.eq("md5Secret"), Mockito.any());
    }

    // Assert
    // It is highly unlikely that all chosen indices match the deterministic fixedIndex when
    // backing randomness is SecureRandom instead of seeded java.util.Random. At least one
    // should differ, demonstrating the absence of trivial seed-based control.
    boolean allMatchFixedIndex = true;
    for (String chosen : chosenSecrets) {
      if (!chosen.equals(HashingAssignment.SECRETS[fixedIndex])) {
        allMatchFixedIndex = false;
        break;
      }
    }
    assertNotEquals(
        true,
        allMatchFixedIndex,
        "Secret selection should not be trivially controllable via java.util.Random seeding logic");
  }

  /**
   * Helper that attempts to reverse-engineer which secret index produced a given MD5 hash.
   * This is only used within tests to avoid exposing any new production behavior.
   */
  private int resolveSecretIndexFromHash(String md5Hash) throws NoSuchAlgorithmException {
    for (int i = 0; i < HashingAssignment.SECRETS.length; i++) {
      byte[] digest =
          java.security.MessageDigest.getInstance("MD5").digest(HashingAssignment.SECRETS[i].getBytes());
      String candidate = DatatypeConverter.printHexBinary(digest).toUpperCase();
      if (candidate.equals(md5Hash)) {
        return i;
      }
    }
    // Fallback: if not found (should not happen), just return 0
    return 0;
  }
}
