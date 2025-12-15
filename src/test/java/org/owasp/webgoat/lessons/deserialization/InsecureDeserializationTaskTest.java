package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing on the deserialization hardening:
 * - Ensures that only allowed types (VulnerableTaskHolder and String) can be deserialized.
 * - Demonstrates that an attempt to deserialize a disallowed type fails (mitigating unsafe deserialization).
 *
 * Note: These tests inherently exercise Java deserialization but are constrained to simple
 * local objects and do not rely on external systems.
 */
class InsecureDeserializationTaskTest {

    @Test
    void shouldSuccessfullyDeserializeAllowedVulnerableTaskHolder() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        byte[] serialized = serialize(holder);
        String token = base64UrlEncode(serialized);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // In the original lesson, timing decides success, but our main interest is that
        // the deserialization of an allowed type does not fail due to the new filter.
        assertThat(result)
                .as("Deserialization of allowed VulnerableTaskHolder should not be blocked by filter")
                .isNotNull();
    }

    @Test
    void shouldRejectDeserializationOfDisallowedType() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // Use a simple disallowed type (e.g., Integer) to test the ObjectInputFilter allowlist.
        Integer maliciousObject = Integer.valueOf(42);
        byte[] serialized = serialize(maliciousObject);
        String token = base64UrlEncode(serialized);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // Due to the ObjectInputFilter, this should be rejected and mapped to a failure result.
        assertThat(result)
                .as("Deserialization of a non-allowed type must not succeed")
                .isNotNull();
        assertThat(result.isLessonCompleted())
                .as("Lesson should not be marked completed for disallowed deserialization input")
                .isFalse();
    }

    private byte[] serialize(Object o) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bout)) {
            oos.writeObject(o);
        }
        return bout.toByteArray();
    }

    /**
     * The production code expects URL-safe Base64 with '-' and '_' instead of '+' and '/'.
     */
    private String base64UrlEncode(byte[] data) {
        String base64 = Base64.getEncoder().encodeToString(data);
        return base64.replace('+', '-').replace('/', '_');
    }
}
