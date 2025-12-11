package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask validating the new SecureObjectInputStream
 * allowlist behavior while preserving the legitimate VulnerableTaskHolder flow.
 */
class InsecureDeserializationTaskDeltaTest {

  @Test
  @DisplayName("completed should successfully handle a valid VulnerableTaskHolder token")
  void completedShouldAcceptValidVulnerableTaskHolderToken() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    VulnerableTaskHolder holder = new VulnerableTaskHolder();
    // Serialize a legitimate VulnerableTaskHolder instance
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(holder);
    }

    String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
    String webToken = b64.replace('+', '-').replace('/', '_');

    // Act
    AttackResult result = task.completed(webToken);

    // Assert
    assertThat(result.getLessonCompleted())
        .as("Valid VulnerableTaskHolder token should still succeed after hardening deserialization")
        .isTrue();
  }
}
