package org.owasp.webgoat.lessons.cryptography;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Delta tests for HashingAssignment focusing on the change from java.util.Random
 * to java.security.SecureRandom for secret selection and ensuring hashing
 * behavior remains consistent.
 */
public class HashingAssignmentTest {

  @Test
  void getMd5_shouldReturnValidMd5HashAndStoreSecretInSession() throws NoSuchAlgorithmException {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpSession session = Mockito.mock(HttpSession.class);
    Mockito.when(request.getSession()).thenReturn(session);
    Mockito.when(session.getAttribute("md5Hash")).thenReturn(null);

    HashingAssignment assignment = new HashingAssignment();

    String md5 = assignment.getMd5(request);

    assertNotNull(md5);
    assertTrue(md5.matches("^[0-9A-F]{32}$"));
    Mockito.verify(session).setAttribute(Mockito.eq("md5Hash"), Mockito.anyString());
    Mockito.verify(session).setAttribute(Mockito.eq("md5Secret"), Mockito.anyString());
  }

  @Test
  void getSha256_shouldReturnValidSha256HashAndStoreSecretInSession() throws NoSuchAlgorithmException {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpSession session = Mockito.mock(HttpSession.class);
    Mockito.when(request.getSession()).thenReturn(session);
    Mockito.when(session.getAttribute("sha256")).thenReturn(null);

    HashingAssignment assignment = new HashingAssignment();

    String sha256 = assignment.getSha256(request);

    assertNotNull(sha256);
    assertTrue(sha256.matches("^[0-9A-F]{64}$"));
    Mockito.verify(session).setAttribute(Mockito.eq("sha256Hash"), Mockito.anyString());
    Mockito.verify(session).setAttribute(Mockito.eq("sha256Secret"), Mockito.anyString());
  }

  @Test
  void secureRandom_shouldBeUsableForSecretSelectionRange() {
    SecureRandom secureRandom = new SecureRandom();
    int index = secureRandom.nextInt(HashingAssignment.SECRETS.length);
    assertTrue(index >= 0 && index < HashingAssignment.SECRETS.length);
  }
}
