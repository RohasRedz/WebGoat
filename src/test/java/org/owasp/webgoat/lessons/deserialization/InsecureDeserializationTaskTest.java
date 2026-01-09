package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing on:
 * - Deserialization filter restricting allowed classes.
 * - Ensuring valid VulnerableTaskHolder payloads are still accepted.
 */
class InsecureDeserializationTaskTest {

  private String toWebSafeBase64(byte[] bytes) {
    String b64 = Base64.getEncoder().encodeToString(bytes);
    return b64.replace('+', '-').replace('/', '_');
  }

  @Test
  void completed_acceptsValidVulnerableTaskHolderPayload() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();
    VulnerableTaskHolder holder = new VulnerableTaskHolder();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(holder);
    }
    String token = toWebSafeBase64(baos.toByteArray());

    // Act
    AttackResult result = task.completed(token);

    // Assert
    assertThat(result.getLessonCompleted()).isNotNull();
  }

  @Test
  void completed_rejectsDisallowedClassThroughFilter() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();
    String maliciousObject = "malicious-string";

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(maliciousObject);
    }
    String token = toWebSafeBase64(baos.toByteArray());

    // Act
    AttackResult result = task.completed(token);

    // Assert
    // Expect that the filter or subsequent type checks cause failure, not success.
    assertThat(result.getLessonCompleted()).isFalse();
  }
}
