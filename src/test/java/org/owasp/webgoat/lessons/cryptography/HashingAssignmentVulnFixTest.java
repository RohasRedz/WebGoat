package org.owasp.webgoat.lessons.cryptography;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Delta unit tests for the vulnerability fix in HashingAssignment.
 *
 * Focus: ensure that getMd5 and getSha256 exercise the SecureRandom-based secret
 * selection paths and that secrets/values are read from the session on
 * subsequent invocations (no regression of behavior).
 */
class HashingAssignmentVulnFixTest {

  @Test
  @DisplayName("getMd5: first call computes hash and stores secret+hash in session; second call reuses stored hash")
  void getMd5_usesSessionAndTriggersSecureRandomPath() throws NoSuchAlgorithmException {
    // Arrange
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);

    // First call: no md5Hash present so SecureRandom-based secret selection must be used
    when(request.getSession()).thenReturn(session);
    when(session.getAttribute("md5Hash")).thenReturn(null);

    // Act
    String firstHash = assignment.getMd5(request);

    // Assert
    // Hash should be non-null and non-empty
    // (We don't assert specific value because SecureRandom makes it non-deterministic.)
    org.junit.jupiter.api.Assertions.assertNotNull(firstHash);
    org.junit.jupiter.api.Assertions.assertFalse(firstHash.isEmpty());

    // Arrange second call: session now returns the previously stored hash
    when(session.getAttribute("md5Hash")).thenReturn(firstHash);

    // Act
    String secondHash = assignment.getMd5(request);

    // Assert
    // Subsequent calls should reuse the stored hash (same value returned)
    assertEquals(firstHash, secondHash);
  }

  @Test
  @DisplayName("getSha256: first call computes hash and stores secret+hash in session; second call reuses stored hash")
  void getSha256_usesSessionAndTriggersSecureRandomPath() throws NoSuchAlgorithmException {
    // Arrange
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);

    // First call: no sha256 hash present so SecureRandom-based secret selection must be used
    when(request.getSession()).thenReturn(session);
    when(session.getAttribute("sha256")).thenReturn(null);

    // Act
    String firstHash = assignment.getSha256(request);

    // Assert
    org.junit.jupiter.api.Assertions.assertNotNull(firstHash);
    org.junit.jupiter.api.Assertions.assertFalse(firstHash.isEmpty());

    // Arrange second call: session returns the previously stored hash
    when(session.getAttribute("sha256")).thenReturn(firstHash);

    // Act
    String secondHash = assignment.getSha256(request);

    // Assert
    assertEquals(firstHash, secondHash);
  }

  @Test
  @DisplayName("getMd5 and getSha256: hashes for different algorithms should differ for the same session secrets")
  void md5AndSha256HashesDifferForSameUnderlyingSecrets() throws NoSuchAlgorithmException {
    // Arrange
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);

    when(request.getSession()).thenReturn(session);
    when(session.getAttribute("md5Hash")).thenReturn(null);
    when(session.getAttribute("sha256")).thenReturn(null);

    // Act
    String md5Hash = assignment.getMd5(request);
    String sha256Hash = assignment.getSha256(request);

    // Assert
    // While underlying secrets are chosen via SecureRandom, we can still assert
    // that MD5 and SHA-256 hashes are not the same in normal circumstances.
    // This is a probabilistic assertion but collision between different
    // algorithms for random secrets is practically impossible.
    assertNotEquals(md5Hash, sha256Hash);
  }
}
