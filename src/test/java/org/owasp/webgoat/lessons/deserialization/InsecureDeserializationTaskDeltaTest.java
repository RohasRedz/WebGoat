package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing only on:
 * - Deserialization is restricted to VulnerableTaskHolder (whitelisting via custom ObjectInputStream).
 * - Deserialization of any other class results in failure and does not succeed.
 */
public class InsecureDeserializationTaskDeltaTest {

    private String toWebGoatToken(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // Mirror the transformation in the controller (token.replace('-', '+').replace('_', '/'))
        return base64.replace('+', '-').replace('/', '_');
    }

    @Test
    void shouldSuccessfullyDeserializeVulnerableTaskHolder() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        VulnerableTaskHolder holder = new VulnerableTaskHolder(); // Assumes default constructor
        String token = toWebGoatToken(holder);

        // Act
        AttackResult result = task.completed(token);

        // Assert: expect lesson success path when a valid VulnerableTaskHolder is provided
        assertThat(result.getLessonCompleted())
                .as("Expected lesson to be marked as completed for valid VulnerableTaskHolder payload")
                .isTrue();
    }

    @Test
    void shouldRejectDeserializationOfNonWhitelistedClass() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        MaliciousPojo malicious = new MaliciousPojo();
        String token = toWebGoatToken(malicious);

        // Act
        AttackResult result = task.completed(token);

        // Assert: whitelisting should cause failure path, not success
        assertThat(result.getLessonCompleted())
                .as("Non-whitelisted classes must not be accepted as valid payloads")
                .isFalse();
    }

    /**
     * Simple serializable class used to simulate a malicious or unexpected payload.
     */
    private static class MaliciousPojo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String data = "malicious";
    }
}
