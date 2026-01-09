package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing on the ObjectInputFilter:
 * - verifies that allowed types (VulnerableTaskHolder) still work.
 * - verifies that disallowed types (e.g., java.lang.Integer) are rejected by the filter.
 */
public class InsecureDeserializationTaskTest {

  private String serializeToWebToken(Object obj) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(obj);
    }
    String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
    // Mirror the token transformation in the controller (replace '+' and '/')
    return base64.replace('+', '-').replace('/', '_');
  }

  @Test
  @DisplayName("completed should succeed when deserializing an allowed VulnerableTaskHolder object")
  void completed_allowsVulnerableTaskHolder() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();
    VulnerableTaskHolder holder = new VulnerableTaskHolder("test");
    String token = serializeToWebToken(holder);

    // Act
    AttackResult result = task.completed(token);

    // Assert: the exercise logic still works for the allowed type;
    // success indicates that the filter did not block VulnerableTaskHolder.
    assertTrue(result.getLessonCompleted(), "Deserialization of allowed type should succeed");
  }

  @Test
  @DisplayName("completed should fail when deserializing a disallowed type due to ObjectInputFilter")
  void completed_rejectsDisallowedType() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();
    Integer maliciousObject = 42; // not in the allowlist
    String token = serializeToWebToken(maliciousObject);

    // Act
    AttackResult result = task.completed(token);

    // Assert: the ObjectInputFilter should prevent successful deserialization
    // of a type that is not in the allowlist, leading to a failed result.
    assertFalse(
        result.getLessonCompleted(),
        "Deserialization of disallowed type should not complete the lesson"
    );
  }
}
