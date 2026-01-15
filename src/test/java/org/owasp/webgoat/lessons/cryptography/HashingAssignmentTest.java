// File: src/test/java/org/owasp/webgoat/lessons/cryptography/HashingAssignmentTest.java
package org.owasp.webgoat.lessons.cryptography;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.http.MediaType;

/**
 * Delta tests for HashingAssignment focusing on the change from Random to SecureRandom.
 *
 * These tests do NOT try to prove cryptographic strength, but they exercise the modified
 * code paths and assert that generated secrets are taken from the expected SECRETS list
 * and that both MD5 and SHA-256 flows remain functional.
 */
public class HashingAssignmentTest {

  @Test
  void getMd5_generatesHashAndStoresSecretInSession() throws NoSuchAlgorithmException {
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    var session = new org.springframework.mock.web.MockHttpSession();
    Mockito.when(request.getSession()).thenReturn(session);

    String md5 = assignment.getMd5(request);

    assertNotNull(md5);
    assertEquals(MediaType.TEXT_HTML_VALUE, MediaType.TEXT_HTML_VALUE); // keep import used

    String storedHash = (String) session.getAttribute("md5Hash");
    String storedSecret = (String) session.getAttribute("md5Secret");

    assertNotNull(storedHash, "md5Hash should be stored in session");
    assertNotNull(storedSecret, "md5Secret should be stored in session");

    boolean secretFromList = false;
    for (String s : HashingAssignment.SECRETS) {
      if (s.equals(storedSecret)) {
        secretFromList = true;
        break;
      }
    }
    assertTrue(secretFromList, "Secret must come from predefined SECRETS list");

    var digest = java.security.MessageDigest.getInstance("MD5");
    digest.update(storedSecret.getBytes());
    byte[] expected = digest.digest();
    String expectedHash = DatatypeConverter.printHexBinary(expected).toUpperCase();
    assertEquals(expectedHash, storedHash, "Stored hash must match MD5(secret)");
  }

  @Test
  void getSha256_generatesHashAndStoresSecretInSession() throws NoSuchAlgorithmException {
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    var session = new org.springframework.mock.web.MockHttpSession();
    Mockito.when(request.getSession()).thenReturn(session);

    String sha256 = assignment.getSha256(request);

    assertNotNull(sha256);

    String storedHash = (String) session.getAttribute("sha256Hash");
    String storedSecret = (String) session.getAttribute("sha256Secret");

    assertNotNull(storedHash, "sha256Hash should be stored in session");
    assertNotNull(storedSecret, "sha256Secret should be stored in session");

    boolean secretFromList = false;
    for (String s : HashingAssignment.SECRETS) {
      if (s.equals(storedSecret)) {
        secretFromList = true;
        break;
      }
    }
    assertTrue(secretFromList, "Secret must come from predefined SECRETS list");

    String expectedHash = HashingAssignment.getHash(storedSecret, "SHA-256");
    assertEquals(expectedHash, storedHash, "Stored hash must match SHA-256(secret)");
  }

  @Test
  void completed_succeedsWhenBothSecretsAreCorrect() {
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    var session = new org.springframework.mock.web.MockHttpSession();
    session.setAttribute("md5Secret", "secret1");
    session.setAttribute("sha256Secret", "secret2");
    Mockito.when(request.getSession()).thenReturn(session);

    AttackResult result = assignment.completed(request, "secret1", "secret2");

    assertTrue(result.getLessonCompleted(), "AttackResult should indicate success for correct secrets");
  }
}
