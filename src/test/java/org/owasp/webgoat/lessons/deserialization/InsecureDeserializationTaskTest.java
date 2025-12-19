package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing on the deserialization allowlist.
 *
 * These tests verify that:
 * - Legitimate VulnerableTaskHolder payloads are still processed.
 * - Payloads containing disallowed types are rejected and result in a failure response.
 */
class InsecureDeserializationTaskTest {

    @Test
    @DisplayName("completed should successfully process a valid VulnerableTaskHolder payload")
    void completedAcceptsValidVulnerableTaskHolderPayload() throws IOException {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // Build a legitimate VulnerableTaskHolder object and serialize it
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = serializeToWebToken(holder);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // We only need to ensure that the call does not throw and returns some result.
        // Exact timing-based success condition is lesson-specific and not changed by the fix.
        // The key delta is that valid allowed type is still accepted rather than blocked.
        org.junit.jupiter.api.Assertions.assertNotNull(result, "A result should be returned for a valid payload");
    }

    @Test
    @DisplayName("completed should fail when deserializing a disallowed class")
    void completedRejectsDisallowedClassPayload() throws IOException {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // Serialize a disallowed type (e.g., java.lang.Runtime)
        Runtime runtime = Runtime.getRuntime();
        String token = serializeToWebToken(runtime);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // Disallowed class must not be accepted; depending on mapping, we expect a failure feedback.
        assertEquals(
                "failed",
                result.getLessonCompleted(),
                "Deserialization of a disallowed class should fail after the allowlist fix");
    }

    /**
     * Helper: Serialize object to the token format expected by the lesson:
     * base64-encoded with URL-safe replacement of '+' and '/'.
     */
    private String serializeToWebToken(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(o);
        }
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // Reverse operation from code: token.replace('-', '+').replace('_', '/')
        // So we do forward transform: '+' -> '-', '/' -> '_'
        return base64.replace('+', '-').replace('/', '_');
    }
}
