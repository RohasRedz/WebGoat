package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing only on the new safe behavior:
 * - Uses ObjectInputFilter to restrict deserializable types.
 * - Still accepts a legitimate VulnerableTaskHolder payload.
 * - Rejects unexpected object types that would previously be deserialized.
 */
class InsecureDeserializationTaskTest {

  private String toBase64Url(byte[] bytes) {
    // The production code expects URL-safe Base64 with '-' and '_' substitutions.
    String b64 = Base64.getEncoder().encodeToString(bytes);
    return b64.replace('+', '-').replace('/', '_');
  }

  @Test
  @DisplayName("completed() should successfully process a legitimate VulnerableTaskHolder token")
  void completedAcceptsAllowedVulnerableTaskHolder() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    VulnerableTaskHolder holder = new VulnerableTaskHolder();
    // Note: We do not depend on internal fields of VulnerableTaskHolder; just type.

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(holder);
    }
    String token = toBase64Url(bos.toByteArray());

    // Act
    AttackResult result = task.completed(token);

    // Assert
    assertTrue(
        result.getLessonCompleted(),
        "A legitimate VulnerableTaskHolder object should still be accepted after the fix");
  }

  @Test
  @DisplayName(
      "completed() should fail when deserializing a disallowed object type due to ObjectInputFilter")
  void completedRejectsDisallowedType() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    // Create a token containing a disallowed type (e.g., plain String) which is NOT VulnerableTaskHolder.
    String payload = "malicious-string-payload";
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(payload);
    }
    String token = toBase64Url(bos.toByteArray());

    // Act
    AttackResult result = task.completed(token);

    // Assert
    assertFalse(
        result.getLessonCompleted(),
        "A token with a non-VulnerableTaskHolder object should not satisfy the assignment");
  }

  @Test
  @DisplayName("completed() should fail on obviously invalid/garbled tokens")
  void completedRejectsInvalidToken() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();
    String invalidToken = "not-a-valid-base64-token";

    // Act
    AttackResult result = task.completed(invalidToken);

    // Assert
    assertFalse(
        result.getLessonCompleted(),
        "Invalid tokens must not result in successful completion after hardening");
  }
}
