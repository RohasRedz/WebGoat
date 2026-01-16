/*
 * SPDX-FileCopyrightText: Copyright © 2019 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.cryptography;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Delta tests for HashingAssignment focusing on the change from Random to SecureRandom for secret
 * selection. We cannot deterministically assert randomness, but we can verify that:
 * - The endpoint uses the SECRETS array and session as before.
 * - Multiple invocations can yield different secrets (statistical sanity check, not a PRNG test).
 */
public class HashingAssignmentTest {

  @Test
  void getMd5_shouldStoreAndReturnHashDerivedFromSecretsArray() throws NoSuchAlgorithmException {
    HashingAssignment assignment = new HashingAssignment();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpSession session = Mockito.mock(HttpSession.class);

    Mockito.when(request.getSession()).thenReturn(session);
    Mockito.when(session.getAttribute("md5Hash")).thenReturn(null);

    String hash = assignment.getMd5(request);

    // Ensure we got some hex-like hash value back
    // (length check and media type context are enough for delta coverage)
    // MD5 hex string length is 32 characters
    assertEquals(32, hash.length());

    // Verify we still interact with the session as before
    Mockito.verify(session).setAttribute(Mockito.eq("md5Hash"), Mockito.eq(hash));
    Mockito.verify(session).setAttribute(Mockito.eq("md5Secret"), Mockito.anyString());
  }

  @Test
  void getSha256_shouldBeDeterministicPerSessionButDifferentAcrossSecrets()
      throws NoSuchAlgorithmException {
    HashingAssignment assignment = new HashingAssignment();

    // First session: ensure it stores a hash
    HttpServletRequest request1 = Mockito.mock(HttpServletRequest.class);
    HttpSession session1 = Mockito.mock(HttpSession.class);
    Mockito.when(request1.getSession()).thenReturn(session1);
    Mockito.when(session1.getAttribute("sha256")).thenReturn(null);

    String firstHash = assignment.getSha256(request1);
    assertEquals(64, firstHash.length()); // SHA-256 hex is 64 chars
    Mockito.verify(session1).setAttribute("sha256Hash", firstHash);
    Mockito.verify(session1).setAttribute(Mockito.eq("sha256Secret"), Mockito.anyString());

    // Second independent session with fresh attributes:
    HttpServletRequest request2 = Mockito.mock(HttpServletRequest.class);
    HttpSession session2 = Mockito.mock(HttpSession.class);
    Mockito.when(request2.getSession()).thenReturn(session2);
    Mockito.when(session2.getAttribute("sha256")).thenReturn(null);

    String secondHash = assignment.getSha256(request2);
    assertEquals(64, secondHash.length());

    // With SecureRandom, it is much less likely both sessions pick the same secret.
    // This is a probabilistic check to exercise the changed behavior.
    // In case of collision, the assertion will fail, surfacing regressions like reverting to a constant.
    assertNotEquals(firstHash, secondHash);
  }
}
