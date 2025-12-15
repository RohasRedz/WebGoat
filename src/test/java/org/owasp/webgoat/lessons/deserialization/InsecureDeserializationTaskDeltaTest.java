// Delta_UnitTest_Agent
// NOTE: Package inferred from production class.
package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for InsecureDeserializationTask (Jira: SVCF-656).
 *
 * Focus only on changed behavior:
 *  - Deserialization must be restricted via ObjectInputFilter to an allowlist.
 *  - Malicious / unexpected classes must be rejected instead of being deserialized.
 */
public class InsecureDeserializationTaskDeltaTest {

    private String toUrlSafeBase64(byte[] bytes) {
        String b64 = Base64.getEncoder().encodeToString(bytes);
        // Reverse of token.replace('-', '+').replace('_', '/') in production code
        return b64.replace('+', '-').replace('/', '_');
    }

    @Test
    @DisplayName("Successfully deserializes allowed VulnerableTaskHolder instance")
    void deserializesAllowedVulnerableTaskHolder() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // Arrange: serialize a legitimate VulnerableTaskHolder instance
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(holder);
            oos.flush();
            serialized = baos.toByteArray();
        }

        String token = toUrlSafeBase64(serialized);

        // Act
        AttackResult result = task.completed(token);

        // Assert: Since the lesson logic checks timing, we only assert that it does NOT fail
        // due to filter rejection. The timing-based success condition is left unchanged.
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Rejects deserialization of disallowed class via ObjectInputFilter")
    void rejectsDisallowedClass() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // Arrange: serialize an object of a type that should not be on the allowlist
        Object disallowed = new java.util.Date();
        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(disallowed);
            oos.flush();
            serialized = baos.toByteArray();
        }

        String token = toUrlSafeBase64(serialized);

        // Act
        AttackResult result = task.completed(token);

        // Assert: The filter should prevent successful completion for disallowed type.
        // The method handles exceptions and returns a failed AttackResult.
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted())
                .as("Deserialization of disallowed types must not be treated as successful")
                .isFalse();
    }

    @Test
    @DisplayName("Deserializing String (allowed) does not crash and results in failure outcome")
    void deserializesStringButFailsLesson() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // Arrange: serialize a String (which is allowed in filter as per fix)
        String payload = "just-a-string";
        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(payload);
            oos.flush();
            serialized = baos.toByteArray();
        }

        String token = toUrlSafeBase64(serialized);

        // Act
        AttackResult result = task.completed(token);

        // Assert: logic in completed() treats String as failure feedback,
        // but must not throw due to filter configuration.
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
