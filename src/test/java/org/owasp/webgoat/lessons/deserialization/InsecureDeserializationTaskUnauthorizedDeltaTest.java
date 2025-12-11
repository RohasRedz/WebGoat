package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Negative delta test verifying that unauthorized types can no longer be deserialized
 * due to the SecureObjectInputStream allowlist, thereby mitigating insecure deserialization.
 */
class InsecureDeserializationTaskUnauthorizedDeltaTest {

  private static class MaliciousObject implements Serializable {
    private static final long serialVersionUID = 1L;
  }

  @Test
  @DisplayName("completed should fail when deserializing an unauthorized class")
  void completedShouldRejectUnauthorizedClassDeserialization() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    MaliciousObject malicious = new MaliciousObject();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(malicious);
    }

    String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
    String webToken = b64.replace('+', '-').replace('/', '_');

    // Act
    AttackResult result = task.completed(webToken);

    // Assert
    assertThat(result.getLessonCompleted())
        .as("Unauthorized class must not be accepted after SecureObjectInputStream hardening")
        .isFalse();
  }
}
