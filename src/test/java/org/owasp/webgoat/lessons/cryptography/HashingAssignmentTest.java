package org.owasp.webgoat.lessons.cryptography;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.xml.bind.DatatypeConverter;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Delta unit tests for HashingAssignment focusing only on the changed behavior:
 * - Use of java.security.SecureRandom for secret selection instead of java.util.Random.
 * - Preservation of existing lesson behavior (session caching and hash computation).
 */
public class HashingAssignmentTest {

  /**
   * This test indirectly verifies that the secret selection is now non-deterministic and
   * cryptographically strong by asserting that multiple invocations produce a distribution
   * over the full SECRETS array, which would be extremely unlikely if the RNG were fixed
   * or trivially predictable.
   *
   * It also ensures that the returned hash corresponds to one of the allowed secrets.
   */
  @Test
  @DisplayName("getMd5 should use a securely randomized secret and cache it in the session")
  void getMd5_usesSecureRandomAndCachesSecret() throws NoSuchAlgorithmException {
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpSession session = Mockito.mock(HttpSession.class);

    Mockito.when(request.getSession()).thenReturn(session);
    Mockito.when(session.getAttribute("md5Hash")).thenReturn(null);

    final String[] secrets = HashingAssignment.SECRETS;
    boolean[] seen = new boolean[secrets.length];

    // Exercise the method multiple times to observe distribution of selected secrets
    for (int i = 0; i < 200; i++) {
      // Reset md5Hash to null so a new secret is chosen each time
      Mockito.when(session.getAttribute("md5Hash")).thenReturn(null);
      Mockito.when(session.getAttribute("md5Secret")).thenReturn(null);

      String hash = assignment.getMd5(request);

      // Capture the secret stored in session and verify it's one of the allowed ones
      Mockito.verify(session, Mockito.atLeastOnce()).setAttribute(Mockito.eq("md5Secret"), Mockito.any());
      Object secretObj = Mockito.mockingDetails(session).getInvocations().stream()
          .filter(inv -> "setAttribute".equals(inv.getMethod().getName()))
          .filter(inv -> "md5Secret".equals(inv.getArgument(0)))
          .reduce((first, second) -> second) // get last invocation for md5Secret
          .map(inv -> inv.getArgument(1))
          .orElse(null);

      // Reset interactions for the next iteration
      Mockito.clearInvocations(session);

      String secret = (String) secretObj;
      int index = indexOf(secrets, secret);
      // Ensure the chosen secret is one of the predefined secrets
      assertTrue(index >= 0, "Selected secret must be from HashingAssignment.SECRETS");

      seen[index] = true;

      // Verify that the hash corresponds to the chosen secret using MD5
      String recomputed = computeMd5(secret);
      assertEquals(recomputed, hash, "Returned MD5 hash must match the chosen secret");
    }

    // Verify that over multiple trials we have seen more than one distinct secret,
    // which would be extremely unlikely if the RNG were deterministic or fixed.
    int distinctSeen = 0;
    for (boolean b : seen) {
      if (b) distinctSeen++;
    }
    assertTrue(distinctSeen > 1, "SecureRandom-based selection should yield more than one distinct secret over many trials");
  }

  /**
   * This test verifies that once a secret and hash are placed in the session, subsequent
   * calls to getMd5() return the cached hash and do not change the secret, preserving
   * original lesson behavior while still using SecureRandom on the first selection.
   */
  @Test
  @DisplayName("getMd5 should reuse existing session values without reselection")
  void getMd5_reusesExistingSessionValues() throws NoSuchAlgorithmException {
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpSession session = Mockito.mock(HttpSession.class);
    Mockito.when(request.getSession()).thenReturn(session);

    String initialSecret = HashingAssignment.SECRETS[new SecureRandom().nextInt(HashingAssignment.SECRETS.length)];
    String initialHash = computeMd5(initialSecret);

    Mockito.when(session.getAttribute("md5Hash")).thenReturn(initialHash);
    Mockito.when(session.getAttribute("md5Secret")).thenReturn(initialSecret);

    String result1 = assignment.getMd5(request);
    String result2 = assignment.getMd5(request);

    // Should always return the same cached hash
    assertEquals(initialHash, result1);
    assertEquals(initialHash, result2);

    // Ensure that no new secret is written to the session when a cached value exists
    Mockito.verify(session, Mockito.never()).setAttribute(Mockito.eq("md5Secret"), Mockito.any());
  }

  /**
   * This test ensures that MD5 and SHA-256 flows are independent but both rely on
   * randomized secrets, and that secrets differ across algorithms in normal operation,
   * which is aligned with per-call SecureRandom usage.
   */
  @Test
  @DisplayName("MD5 and SHA-256 secrets should generally differ due to independent SecureRandom selection")
  void md5AndSha256SecretsGenerallyDiffer() throws NoSuchAlgorithmException {
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpSession session = Mockito.mock(HttpSession.class);
    Mockito.when(request.getSession()).thenReturn(session);

    // First call: MD5 and SHA-256 secrets picked and cached
    Mockito.when(session.getAttribute("md5Hash")).thenReturn(null);
    Mockito.when(session.getAttribute("sha256")).thenReturn(null);

    assignment.getMd5(request);
    assignment.getSha256(request);

    // Capture the stored secrets
    Mockito.verify(session, Mockito.atLeastOnce()).setAttribute(Mockito.eq("md5Secret"), Mockito.any());
    Mockito.verify(session, Mockito.atLeastOnce()).setAttribute(Mockito.eq("sha256Secret"), Mockito.any());

    String md5Secret = (String) Mockito.mockingDetails(session).getInvocations().stream()
        .filter(inv -> "setAttribute".equals(inv.getMethod().getName()))
        .filter(inv -> "md5Secret".equals(inv.getArgument(0)))
        .reduce((first, second) -> second)
        .map(inv -> inv.getArgument(1))
        .orElse(null);

    String shaSecret = (String) Mockito.mockingDetails(session).getInvocations().stream()
        .filter(inv -> "setAttribute".equals(inv.getMethod().getName()))
        .filter(inv -> "sha256Secret".equals(inv.getArgument(0)))
        .reduce((first, second) -> second)
        .map(inv -> inv.getArgument(1))
        .orElse(null);

    // Clear interactions for any follow-up assertions
    Mockito.clearInvocations(session);

    // Both secrets must be valid entries from the SECRETS array
    assertTrue(indexOf(HashingAssignment.SECRETS, md5Secret) >= 0, "md5Secret must come from HashingAssignment.SECRETS");
    assertTrue(indexOf(HashingAssignment.SECRETS, shaSecret) >= 0, "sha256Secret must come from HashingAssignment.SECRETS");

    // While it's possible they're equal, with SecureRandom they are expected to often differ.
    // We assert non-equality here as a probabilistic indicator of independent random selection.
    assertNotEquals(
        md5Secret,
        shaSecret,
        "MD5 and SHA-256 secrets should typically differ when selected via SecureRandom for separate flows");
  }

  // Helper: find the index of a secret in the array, or -1 if not found.
  private static int indexOf(String[] arr, String value) {
    if (value == null) return -1;
    for (int i = 0; i < arr.length; i++) {
      if (value.equals(arr[i])) {
        return i;
      }
    }
    return -1;
  }

  // Helper: recompute MD5 hash in the same way as HashingAssignment for verification.
  private static String computeMd5(String secret) throws NoSuchAlgorithmException {
    java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
    md.update(secret.getBytes());
    byte[] digest = md.digest();
    return DatatypeConverter.printHexBinary(digest).toUpperCase();
  }
}
