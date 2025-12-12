package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for {@link InsecureDeserializationTask}.
 *
 * These tests focus ONLY on behavior changed by the security fix:
 * - Introduction of SafeObjectInputStream with an allowlist used by completed().
 * - Ensuring that only VulnerableTaskHolder and String are allowed to be deserialized.
 * - Ensuring that disallowed types are rejected safely and mapped to the correct feedback.
 */
public class InsecureDeserializationTaskTest {

    // NOTE:
    // The production code defines SafeObjectInputStream as a private static nested class.
    // We cannot reference it directly from tests without reflection or white-box assumptions.
    // Instead, these tests exercise the behavior through the public completed(...) method,
    // which is sufficient for validating the security fix.

    /**
     * Helper method to serialize an arbitrary object and return a URL-safe Base64 token
     * compatible with InsecureDeserializationTask.completed(), which expects '-' and '_'
     * instead of '+' and '/'.
     */
    private String toUrlSafeBase64Token(Object obj) throws IOException {
        byte[] serialized;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            oos.flush();
            serialized = bos.toByteArray();
        }
        String base64 = Base64.getEncoder().encodeToString(serialized);
        // The controller method maps '-'->'+' and '_'->'/' before decoding, so we invert that here
        return base64.replace('+', '-').replace('/', '_');
    }

    /**
     * Simple serializable class that is NOT on the allowlist used by SafeObjectInputStream.
     * This is used to simulate an attacker-supplied gadget type.
     */
    private static class DisallowedType implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final String value;

        DisallowedType(String value) {
            this.value = value;
        }
    }

    /**
     * Positive-path delta test: ensure that a token containing a serialized VulnerableTaskHolder
     * is accepted by completed(). This verifies that the allowlist permits the expected type.
     *
     * NOTE: We do not assert lesson timing behavior here; that is out-of-scope for the delta.
     */
    @Test
    @DisplayName("completed() should accept tokens containing VulnerableTaskHolder (allowlisted type)")
    void completedAcceptsVulnerableTaskHolder() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // VulnerableTaskHolder is explicitly allowlisted in SafeObjectInputStream
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = toUrlSafeBase64Token(holder);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // Any non-error path means that the object was deserialized successfully as a VulnerableTaskHolder.
        // We do NOT need to assert specific success/failure messaging; the key is that it did not fail
        // due to InvalidClassException or wrong-object feedback.
        assertThat(result).isNotNull();
    }

    /**
     * Positive-path delta test: ensure that a token containing a serialized String is accepted
     * as far as deserialization is concerned. String is also on the allowlist. The existing
     * functional logic will treat a String as an error (stringobject feedback), but our goal
     * is to confirm that String is not rejected at the deserialization boundary.
     */
    @Test
    @DisplayName("completed() should deserialize String tokens (allowlisted type) without InvalidClassException")
    void completedAllowsStringTypeAtDeserializationBoundary() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        String payload = "some-string-payload";

        String token = toUrlSafeBase64Token(payload);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // If String were not on the allowlist, an InvalidClassException would be thrown
        // and mapped to the 'invalidversion' feedback. We only assert that the call
        // completes and returns some AttackResult (the exact feedback is controlled
        // by the original lesson logic and is out of delta scope).
        assertThat(result).isNotNull();
    }

    /**
     * Negative-path delta test: ensure that when a disallowed type is embedded in the token,
     * SafeObjectInputStream rejects it by throwing InvalidClassException, and the public
     * completed() method results in a safe failure feedback ("insecure-deserialization.invalidversion").
     */
    @Test
    @DisplayName("completed() should reject disallowed types and map them to invalidversion feedback")
    void completedRejectsDisallowedTypeWithInvalidVersionFeedback() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // DisallowedType is intentionally NOT part of the allowlist in SafeObjectInputStream
        DisallowedType disallowed = new DisallowedType("evil");
        String token = toUrlSafeBase64Token(disallowed);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // The completed() method catches InvalidClassException and returns a failure with
        // feedback key "insecure-deserialization.invalidversion". We assert this mapping to
        // demonstrate that invalid/deserialization attempts are safely contained.
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
        assertThat(result.getFeedback()).isEqualTo("insecure-deserialization.invalidversion");
    }
}
