package org.owasp.webgoat.lessons.cryptography;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Delta tests for HashingAssignment focusing on the behavior impacted by the PRNG change
 * (java.util.Random -> java.security.SecureRandom). These tests assert endpoint behavior
 * and session interactions remain consistent.
 */
public class HashingAssignmentTest {

    private HashingAssignment hashingAssignment;

    @BeforeEach
    void setUp() {
        hashingAssignment = new HashingAssignment();
    }

    @Test
    void getMd5_shouldStoreSecretAndReturnSameHashFromSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/crypto/hashing/md5");
        request.setContentType(MediaType.TEXT_HTML_VALUE);

        String firstHash = hashingAssignment.getMd5(request);
        assertNotNull(firstHash, "First MD5 hash should not be null");

        // Ensure hash stored in session matches returned value
        String sessionHash = (String) request.getSession().getAttribute("md5Hash");
        assertEquals(firstHash, sessionHash, "Returned MD5 hash must match session-stored hash");

        // Calling again should reuse the same value from session, not compute a new one
        String secondHash = hashingAssignment.getMd5(request);
        assertEquals(firstHash, secondHash, "Subsequent MD5 calls must return the same session value");
    }

    @Test
    void getSha256_shouldStoreSecretAndReturnSameHashFromSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/crypto/hashing/sha256");
        request.setContentType(MediaType.TEXT_HTML_VALUE);

        String firstHash = hashingAssignment.getSha256(request);
        assertNotNull(firstHash, "First SHA-256 hash should not be null");

        String sessionHash = (String) request.getSession().getAttribute("sha256Hash");
        assertEquals(firstHash, sessionHash, "Returned SHA-256 hash must match session-stored hash");

        String secondHash = hashingAssignment.getSha256(request);
        assertEquals(firstHash, secondHash, "Subsequent SHA-256 calls must return the same session value");
    }

    @Test
    void completed_shouldReturnSuccessWhenBothSecretsMatchSession() throws NoSuchAlgorithmException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        // Prepare known secrets and hashes to simulate what getMd5/getSha256 would do
        String md5Secret = "secret1";
        String sha256Secret = "secret2";

        String md5Hash = computeHash(md5Secret, "MD5");
        String sha256Hash = HashingAssignment.getHash(sha256Secret, "SHA-256");

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("md5Secret")).thenReturn(md5Secret);
        when(session.getAttribute("sha256Secret")).thenReturn(sha256Secret);
        when(session.getAttribute("md5Hash")).thenReturn(md5Hash);
        when(session.getAttribute("sha256Hash")).thenReturn(sha256Hash);

        AttackResult result = hashingAssignment.completed(request, md5Secret, sha256Secret);

        assertTrue(result.getLessonCompleted(), "Both correct secrets should complete the lesson successfully");
    }

    @Test
    void completed_shouldIndicatePartialSuccessWhenOnlyOneSecretMatches() throws NoSuchAlgorithmException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        String md5Secret = "secret1";
        String sha256Secret = "secret2";

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("md5Secret")).thenReturn(md5Secret);
        when(session.getAttribute("sha256Secret")).thenReturn(sha256Secret);

        // Case 1: only MD5 secret matches
        AttackResult result1 = hashingAssignment.completed(request, md5Secret, "wrong");
        assertFalse(result1.getLessonCompleted(), "Lesson should not be completed when only one secret matches");

        // Case 2: only SHA-256 secret matches
        AttackResult result2 = hashingAssignment.completed(request, "wrong", sha256Secret);
        assertFalse(result2.getLessonCompleted(), "Lesson should not be completed when only one secret matches");
    }

    @Test
    void completed_shouldFailWhenSecretsAreNullOrIncorrect() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("md5Secret")).thenReturn("secret1");
        when(session.getAttribute("sha256Secret")).thenReturn("secret2");

        AttackResult result = hashingAssignment.completed(request, null, null);
        assertFalse(result.getLessonCompleted(), "Null answers should not complete the lesson");

        AttackResult resultWrong = hashingAssignment.completed(request, "wrong1", "wrong2");
        assertFalse(resultWrong.getLessonCompleted(), "Both wrong answers should not complete the lesson");
    }

    private String computeHash(String secret, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(secret.getBytes());
        byte[] digest = md.digest();
        return DatatypeConverter.printHexBinary(digest).toUpperCase();
    }
}
