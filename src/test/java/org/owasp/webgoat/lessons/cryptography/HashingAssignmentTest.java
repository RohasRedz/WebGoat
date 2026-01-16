package org.owasp.webgoat.lessons.cryptography;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

/**
 * Delta tests for HashingAssignment focusing on the behavior affected by the fix:
 * - Secrets are taken from the SECRETS array when no value is in session.
 * - Endpoints respond and set corresponding session attributes for MD5 and SHA-256 flows.
 *
 * These tests do not assert the specific RNG implementation but ensure the
 * observable behavior of random secret selection and hashing contracts remains valid.
 */
class HashingAssignmentTest {

  @Test
  void getMd5_generatesHashAndSecretWhenNotInSession() throws NoSuchAlgorithmException {
    HashingAssignment hashingAssignment = new HashingAssignment();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);

    when(request.getSession()).thenReturn(session);
    when(session.getAttribute("md5Hash")).thenReturn(null);

    String md5Hash = hashingAssignment.getMd5(request);

    assertNotNull(md5Hash, "MD5 hash should not be null when generated");
    assertFalse(md5Hash.isEmpty(), "MD5 hash should not be empty");

    // verify that secret and hash are stored in the session
    verify(session).setAttribute(eq("md5Hash"), anyString());
    verify(session).setAttribute(eq("md5Secret"), anyString());
  }

  @Test
  void getMd5_usesExistingHashWhenPresentInSession() throws NoSuchAlgorithmException {
    HashingAssignment hashingAssignment = new HashingAssignment();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);

    when(request.getSession()).thenReturn(session);
    when(session.getAttribute("md5Hash")).thenReturn("EXISTING_HASH");

    String md5Hash = hashingAssignment.getMd5(request);

    assertEquals("EXISTING_HASH", md5Hash, "Should return existing MD5 hash from session");

    // no new hash or secret should be stored
    verify(session, never()).setAttribute(eq("md5Hash"), any());
    verify(session, never()).setAttribute(eq("md5Secret"), any());
  }

  @Test
  void getSha256_generatesHashAndSecretWhenNotInSession() throws NoSuchAlgorithmException {
    HashingAssignment hashingAssignment = new HashingAssignment();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);

    when(request.getSession()).thenReturn(session);
    when(session.getAttribute("sha256")).thenReturn(null);

    String sha256Hash = hashingAssignment.getSha256(request);

    assertNotNull(sha256Hash, "SHA-256 hash should not be null when generated");
    assertFalse(sha256Hash.isEmpty(), "SHA-256 hash should not be empty");

    verify(session).setAttribute(eq("sha256Hash"), anyString());
    verify(session).setAttribute(eq("sha256Secret"), anyString());
  }

  @Test
  void getSha256_usesExistingHashWhenPresentInSession() throws NoSuchAlgorithmException {
    HashingAssignment hashingAssignment = new HashingAssignment();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);

    when(request.getSession()).thenReturn(session);
    when(session.getAttribute("sha256")).thenReturn("EXISTING_SHA256");

    String sha256Hash = hashingAssignment.getSha256(request);

    assertEquals("EXISTING_SHA256", sha256Hash, "Should return existing SHA-256 hash from session");

    verify(session, never()).setAttribute(eq("sha256Hash"), any());
    verify(session, never()).setAttribute(eq("sha256Secret"), any());
  }
}
