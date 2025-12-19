// Assuming package based on source path; adjust if actual package differs.
package org.owasp.webgoat.lessons.cryptography;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Delta tests for HashingAssignment focusing only on the changed behavior:
 * - Use of SecureRandom instead of Random to pick the secret.
 *
 * We cannot directly assert the RNG type, so we test externally observable behavior:
 * - Secrets remain limited to the SECRETS array.
 * - Multiple invocations with a fresh session tend to choose different secrets,
 *   indicating non-trivial randomness and that the selection logic is exercised.
 */
class HashingAssignmentTest {

  private HttpServletRequest mockRequestWithFreshSession() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpSession session = Mockito.mock(HttpSession.class);

    Mockito.when(request.getSession()).thenReturn(session);
    Mockito.when(session.getAttribute(Mockito.anyString())).thenReturn(null);

    return request;
  }

  @Test
  @DisplayName("getMd5 should select a secret only from SECRETS and store it in session")
  void getMd5_usesSecretFromWhitelistedSet() throws NoSuchAlgorithmException {
    HashingAssignment assignment = new HashingAssignment();
    HttpServletRequest request = mockRequestWithFreshSession();
    HttpSession session = request.getSession();

    String md5Hash = assignment.getMd5(request);

    assertNotNull(md5Hash, "MD5 hash should not be null");

    Mockito.verify(session).setAttribute(Mockito.eq("md5Hash"), Mockito.any());
    Mockito.verify(session).setAttribute(Mockito.eq("md5Secret"), Mockito.any());

    Mockito.verify(session, Mockito.atLeast(1))
        .setAttribute(Mockito.eq("md5Secret"), Mockito.argThat(secret -> {
          for (String allowed : HashingAssignment.SECRETS) {
            if (allowed.equals(secret)) {
              return true;
            }
          }
          return false;
        }));
  }

  @Test
  @DisplayName("getMd5 should exhibit non-trivial randomness across sessions (SecureRandom path exercised)")
  void getMd5_exercisesSecureRandomSelection() throws NoSuchAlgorithmException {
    HashingAssignment assignment = new HashingAssignment();

    Set<String> observedSecrets = new HashSet<>();

    // We invoke getMd5 with multiple fresh sessions to sample the RNG-driven secret selection.
    IntStream.range(0, 20)
        .forEach(i -> {
          HttpServletRequest request = mockRequestWithFreshSession();
          HttpSession session = request.getSession();

          try {
            assignment.getMd5(request);
          } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
          }

          Mockito.verify(session, Mockito.atLeastOnce())
              .setAttribute(Mockito.eq("md5Secret"), Mockito.any());

          Object capturedSecret =
              Mockito.mockingDetails(session).getInvocations().stream()
                  .filter(inv ->
                      "setAttribute".equals(inv.getMethod().getName())
                          && inv.getArguments().length == 2
                          && "md5Secret".equals(inv.getArgument(0)))
                  .map(inv -> inv.getArgument(1))
                  .reduce((first, second) -> second)
                  .orElse(null);

          if (capturedSecret instanceof String) {
            observedSecrets.add((String) capturedSecret);
          }

          Mockito.clearInvocations(session);
        });

    // With SecureRandom, the likelihood of always picking the same secret over many trials is low.
    // This is a statistical/delta check, not a strict randomness test.
    // At least 2 distinct secrets should be observed under normal operation.
    // If this ever fails due to extreme randomness corner cases, the test can be relaxed.
    assertEquals(true, observedSecrets.size() >= 2,
        "Expected at least 2 distinct secrets over multiple invocations, " +
            "indicating the SecureRandom-driven selection path is exercised");
  }

  @Test
  @DisplayName("getSha256 should select a secret only from SECRETS and store it in session")
  void getSha256_usesSecretFromWhitelistedSet() throws NoSuchAlgorithmException {
    HashingAssignment assignment = new HashingAssignment();
    HttpServletRequest request = mockRequestWithFreshSession();
    HttpSession session = request.getSession();

    String sha256 = assignment.getSha256(request);

    assertNotNull(sha256, "SHA-256 hash should not be null");

    Mockito.verify(session).setAttribute(Mockito.eq("sha256Hash"), Mockito.any());
    Mockito.verify(session).setAttribute(Mockito.eq("sha256Secret"), Mockito.any());

    Mockito.verify(session, Mockito.atLeast(1))
        .setAttribute(Mockito.eq("sha256Secret"), Mockito.argThat(secret -> {
          for (String allowed : HashingAssignment.SECRETS) {
            if (allowed.equals(secret)) {
              return true;
            }
          }
          return false;
        }));
  }
}
