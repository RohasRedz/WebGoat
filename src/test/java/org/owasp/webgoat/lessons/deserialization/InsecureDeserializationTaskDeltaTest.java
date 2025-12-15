// Test file path suggestion:
// src/test/java/org/owasp/webgoat/lessons/deserialization/InsecureDeserializationTaskDeltaTest.java

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
 * Delta tests for InsecureDeserializationTask focusing only on the changed behavior:
 *
 * 1) Deserialization of a valid, whitelisted VulnerableTaskHolder-based token still works
 *    and follows the success timing path.
 * 2) Deserialization of a disallowed type (non-whitelisted class) is rejected and mapped
 *    to an existing failure feedback path (invalidversion).
 *
 * These tests are not a full regression suite; they are narrowly scoped to the
 * ObjectInputFilter-based deserialization hardening that was added.
 */
public class InsecureDeserializationTaskDeltaTest {

    /**
     * Helper to serialize an object to the Base64-URL-encoded form expected by the endpoint:
     * - Serialized bytes are Base64-encoded.
     * - '+' is replaced with '-' and '/' is replaced with '_' (URL-safe variant),
     *   mirroring the reverse transformation in the production code.
     */
    private String toWebGoatToken(Object object) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
        }
        String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        return b64.replace('+', '-').replace('/', '_');
    }

    @Test
    @DisplayName("completed() should still accept and successfully process a whitelisted VulnerableTaskHolder token")
    void completedShouldSucceedForValidWhitelistedToken() throws Exception {
        // Arrange
        InsecureDeserializationTask endpoint = new InsecureDeserializationTask();

        // NOTE: We rely on the real VulnerableTaskHolder implementation being available on the classpath.
        // The timing-based logic in the production code measures execution time between readObject()
        // and the instanceof checks. We do not assert exact timing here, only that the happy-path
        // result is success when a valid, whitelisted object is supplied.
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = toWebGoatToken(holder);

        // Act
        AttackResult result = endpoint.completed(token);

        // Assert
        assertNotNull(result, "AttackResult should not be null for a valid token");
        assertTrue(
                result.getLessonCompleted(),
                "Lesson should be marked as completed for a valid VulnerableTaskHolder token"
        );
    }

    /**
     * Dummy non-whitelisted gadget class to simulate an attacker-controlled payload.
     * This class is intentionally not part of the ObjectInputFilter allowlist configured
     * in InsecureDeserializationTask, so deserialization should be rejected.
     */
    private static class MaliciousGadget implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unused")
        private String payload = "evil";
    }

    @Test
    @DisplayName("completed() should reject deserialization of non-whitelisted classes and return a failure result")
    void completedShouldRejectNonWhitelistedType() throws Exception {
        // Arrange
        InsecureDeserializationTask endpoint = new InsecureDeserializationTask();

        // Serialize a non-whitelisted type (MaliciousGadget). The ObjectInputFilter
        // configured in the updated production code should block this during readObject().
        MaliciousGadget gadget = new MaliciousGadget();
        String token = toWebGoatToken(gadget);

        // Act
        AttackResult result = endpoint.completed(token);

        // Assert
        assertNotNull(result, "AttackResult should not be null even when deserialization is rejected");
        assertFalse(
                result.getLessonCompleted(),
                "Lesson must not be completed when a non-whitelisted type is deserialized"
        );

        // The updated code maps generic deserialization/filter failures into the
        // 'insecure-deserialization.invalidversion' feedback pathway. We assert that
        // we hit a known failure feedback key rather than success.
        assertEquals(
                "insecure-deserialization.invalidversion",
                result.getFeedback(),
                "Non-whitelisted payloads should be mapped to the invalidversion failure feedback"
        );
    }
}
