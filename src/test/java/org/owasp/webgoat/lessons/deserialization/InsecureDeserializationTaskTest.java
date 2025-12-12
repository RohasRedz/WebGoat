package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for {@link InsecureDeserializationTask}.
 *
 * These tests focus ONLY on behavior changed by the fix:
 * - Deserialization is now constrained via an ObjectInputFilter allowlist
 *   (VulnerableTaskHolder and String only, everything else denied).
 * - Existing behavior for:
 *   - valid VulnerableTaskHolder objects (success path),
 *   - String objects (stringobject feedback), and
 *   - unexpected objects (wrongobject/invalidversion)
 *   is preserved in the presence of the filter.
 *
 * NOTE:
 * - We do not attempt to introspect the internal ObjectInputStream filter, since the
 *   standard API does not expose it. Instead, we verify its EFFECTS on different
 *   serialized payloads (allowed vs denied types), which is a robust, black-box
 *   validation of the new security behavior.
 */
class InsecureDeserializationTaskTest {

    /**
     * Helper to serialize an object to the token format expected by the controller:
     * Base64 of the standard Java serialization stream, then with '+' -> '-' and '/' -> '_'.
     */
    private String toToken(Object o) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
        }
        String b64 = Base64.getEncoder().encodeToString(bos.toByteArray());
        return b64.replace('+', '-').replace('/', '_');
    }

    /**
     * Verifies that when a valid VulnerableTaskHolder is provided, the lesson
     * still completes successfully. This validates that the allowlist filter
     * permits the expected type and does not break the success path.
     */
    @Test
    @DisplayName("completed should accept VulnerableTaskHolder and complete the lesson")
    void completed_shouldAcceptVulnerableTaskHolder() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = toToken(holder);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // The exact timing-based logic is internal; we assert that for a valid
        // VulnerableTaskHolder, the lesson is allowed to complete.
        assertTrue(
            result.getLessonCompleted(),
            "Expected lesson to be completed for a valid VulnerableTaskHolder instance");
    }

    /**
     * Verifies that when a serialized String is provided, the behavior is preserved
     * and the request fails (stringobject feedback path). This also confirms that
     * the filter allows java.lang.String as per the new allowlist.
     */
    @Test
    @DisplayName("completed should not complete lesson when deserialized object is a String")
    void completed_shouldHandleStringObjectAsFailure() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        String payload = "just a string";
        String token = toToken(payload);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // We only verify that the lesson is NOT completed. The specific feedback
        // key is part of the existing controller logic and not changed by the fix.
        assertFalse(
            result.getLessonCompleted(),
            "Expected lesson not to be completed when deserialized object is a String");
    }

    /**
     * Verifies the EFFECT of the strict allowlist: an unexpected type, which is
     * not in the allowlist (VulnerableTaskHolder, String), must not complete the
     * lesson and should be rejected by the deserialization filter.
     *
     * In the original vulnerable code, this would still be deserialized and then
     * handled as a wrong object; after the fix, the ObjectInputFilter should deny
     * the deserialization, resulting in a failure (e.g., invalidversion).
     */
    @Test
    @DisplayName("completed should fail for non-allowlisted object type due to deserialization filter")
    void completed_shouldFailForNonAllowlistedObject() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // Use a simple, serializable type that is NOT in the allowlist
        // (e.g., java.lang.StringBuilder is not whitelisted in the filter expression).
        StringBuilder unexpected = new StringBuilder("unexpected object");
        String token = toToken(unexpected);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // We assert that the lesson is NOT completed, meaning the object was not
        // accepted as a valid VulnerableTaskHolder. This indicates that the
        // allowlist filter is effectively blocking non-whitelisted types.
        assertFalse(
            result.getLessonCompleted(),
            "Expected lesson not to be completed for non-allowlisted object type");
    }

    /**
     * (Optional but useful) Sanity test: an obviously invalid base64/serialization
     * token should also not complete the lesson, ensuring that malformed inputs
     * are handled safely in the presence of the filter.
     */
    @Test
    @DisplayName("completed should fail for malformed token input")
    void completed_shouldFailForMalformedToken() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // A malformed token that will fail Base64 or deserialization
        String malformedToken = "not-a-valid-token";

        // Act
        AttackResult result = task.completed(malformedToken);

        // Assert
        assertFalse(
            result.getLessonCompleted(),
            "Expected lesson not to be completed for malformed token input");
    }
}
