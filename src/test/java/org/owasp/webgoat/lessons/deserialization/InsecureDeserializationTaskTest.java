package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for InsecureDeserializationTask focusing only on the new ObjectInputFilter-based
 * restriction behavior.
 *
 * These tests verify:
 * - A whitelisted type (VulnerableTaskHolder) can still be deserialized successfully.
 * - A non-whitelisted type causes the request to fail, demonstrating that arbitrary types are
 *   blocked and the insecure-deserialization vulnerability is mitigated.
 */
class InsecureDeserializationTaskTest {

  // TODO: Adjust fields/methods if VulnerableTaskHolder has a different API or requires specific state.
  private VulnerableTaskHolder newVulnerableTaskHolder() {
    return new VulnerableTaskHolder();
  }

  /**
   * Helper to serialize an object to the URL-safe Base64 token format expected by
   * InsecureDeserializationTask: '+' -> '-', '/' -> '_'.
   */
  private String toUrlSafeToken(Object o) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(o);
    }
    String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
    // In the application, incoming token does: token.replace('-', '+').replace('_', '/')
    // so we reverse that mapping here to produce the expected input format.
    return b64.replace('+', '-').replace('/', '_');
  }

  @Test
  void completed_AllowsWhitelistedVulnerableTaskHolder() throws Exception {
    // Arrange: serialize a whitelisted type VulnerableTaskHolder
    String token = toUrlSafeToken(newVulnerableTaskHolder());
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    // Act
    AttackResult result = task.completed(token);

    // Assert: the request should be considered successful (filter allows this type).
    assertTrue(result.getLessonCompleted(), "Whitelisted VulnerableTaskHolder should be accepted");
  }

  @Test
  void completed_RejectsNonWhitelistedType() throws Exception {
    // Arrange: serialize a non-whitelisted type (e.g., Integer)
    String token = toUrlSafeToken(Integer.valueOf(42));
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    // Act
    AttackResult result = task.completed(token);

    // Assert: the request should fail because the ObjectInputFilter rejects this type.
    assertFalse(
        result.getLessonCompleted(),
        "Non-whitelisted type should be rejected by the ObjectInputFilter");
  }
}
