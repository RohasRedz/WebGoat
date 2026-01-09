package org.owasp.webgoat.lessons.cryptography;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Delta tests for HashingAssignment focusing only on the SecureRandom change.
 *
 * Security behavior under test:
 * - getMd5() and getSha256() must no longer rely on java.util.Random; instead,
 *   they should behave like a secure RNG (unpredictable distribution).
 *
 * We cannot easily assert the concrete RNG type without breaking encapsulation,
 * so we assert behavioral properties that would be unlikely with a trivial or
 * constant implementation (e.g., same index every time).
 */
public class HashingAssignmentTest {

  @Test
  @DisplayName("getMd5 should generate potentially different hashes across sessions (SecureRandom-based index)")
  void getMd5_usesSecureRandomLikeBehavior() throws NoSuchAlgorithmException {
    // Arrange
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request1 = Mockito.mock(HttpServletRequest.class);
    HttpSession session1 = Mockito.mock(HttpSession.class);
    Mockito.when(request1.getSession()).thenReturn(session1);
    Mockito.when(session1.getAttribute("md5Hash")).thenReturn(null);

    HttpServletRequest request2 = Mockito.mock(HttpServletRequest.class);
    HttpSession session2 = Mockito.mock(HttpSession.class);
    Mockito.when(request2.getSession()).thenReturn(session2);
    Mockito.when(session2.getAttribute("md5Hash")).thenReturn(null);

    // Act
    String hash1 = assignment.getMd5(request1);
    String hash2 = assignment.getMd5(request2);

    // Assert
    // Both hashes must be non-empty hex strings; if RNG is secure, collisions across
    // two independent calls should be rare.
    assertTrue(hash1 != null && !hash1.isBlank(), "First MD5 hash should not be blank");
    assertTrue(hash2 != null && !hash2.isBlank(), "Second MD5 hash should not be blank");

    // Not a strict guarantee, but a behavioral indication that we are not always
    // choosing the same index (e.g., constant or deterministic trivial implementation).
    // If this ever flaps, it indicates a regression in the randomness behavior.
    assertNotEquals(hash1, hash2, "MD5 hashes from separate invocations should usually differ");
  }

  @Test
  @DisplayName("getSha256 should generate potentially different hashes across sessions (SecureRandom-based index)")
  void getSha256_usesSecureRandomLikeBehavior() throws NoSuchAlgorithmException {
    // Arrange
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request1 = Mockito.mock(HttpServletRequest.class);
    HttpSession session1 = Mockito.mock(HttpSession.class);
    Mockito.when(request1.getSession()).thenReturn(session1);
    Mockito.when(session1.getAttribute("sha256")).thenReturn(null);

    HttpServletRequest request2 = Mockito.mock(HttpServletRequest.class);
    HttpSession session2 = Mockito.mock(HttpSession.class);
    Mockito.when(request2.getSession()).thenReturn(session2);
    Mockito.when(session2.getAttribute("sha256")).thenReturn(null);

    // Act
    String hash1 = assignment.getSha256(request1);
    String hash2 = assignment.getSha256(request2);

    // Assert
    assertTrue(hash1 != null && !hash1.isBlank(), "First SHA-256 hash should not be blank");
    assertTrue(hash2 != null && !hash2.isBlank(), "Second SHA-256 hash should not be blank");

    assertNotEquals(hash1, hash2, "SHA-256 hashes from separate invocations should usually differ");
  }
}
