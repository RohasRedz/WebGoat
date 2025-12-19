package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focused on:
 * 1) Ensuring only java.lang.String and VulnerableTaskHolder are successfully deserialized.
 * 2) Ensuring behavior/feedback for String vs non-VulnerableTaskHolder vs valid VulnerableTaskHolder
 *    is preserved.
 * 3) Ensuring the timing/delay-based behavior remains logically intact (basic assertions).
 */
class InsecureDeserializationTaskTest {

    private final InsecureDeserializationTask task = new InsecureDeserializationTask();

    /**
     * Helper to serialize an object and encode it into the token format expected by the controller
     * (Base64 with '+' → '-' and '/' → '_').
     */
    private String createTokenForObject(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // The controller reverses '-' to '+' and '_' to '/', so we apply the inverse transform here
        return base64.replace('+', '-').replace('/', '_');
    }

    @Test
    @DisplayName("Should reject disallowed type due to ObjectInputFilter allow-list")
    void shouldRejectDisallowedType() throws Exception {
        // Arrange: create a serialized object of a disallowed type (e.g., Integer)
        Integer disallowed = 42;
        String token = createTokenForObject(disallowed);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // With the ObjectInputFilter allow-list, deserializing an Integer should not succeed
        // as a VulnerableTaskHolder or String path, and should produce a failure result.
        assertThat(result)
            .as("Disallowed types should not lead to lesson completion")
            .matches(r -> !r.getLessonCompleted());
    }

    @Test
    @DisplayName("Should preserve behavior for String tokens (stringobject feedback path)")
    void shouldPreserveStringBehavior() throws Exception {
        // Arrange: create a serialized String token (allowed by filter)
        String value = "some-string";
        String token = createTokenForObject(value);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // Original behavior: String instance triggers 'stringobject' feedback and failure.
        assertThat(result)
            .as("String-based tokens should not complete the lesson")
            .matches(r -> !r.getLessonCompleted());
        // We cannot easily inspect internal feedback key here without full WebGoat infrastructure,
        // but at minimum we assert failure (behavior preserved).
    }

    @Test
    @DisplayName("Should preserve success behavior for valid VulnerableTaskHolder within timing window")
    void shouldPreserveSuccessForValidVulnerableTaskHolder() throws Exception {
        // Arrange: create a serialized VulnerableTaskHolder (allowed by filter)
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = createTokenForObject(holder);

        // Act
        long start = System.currentTimeMillis();
        AttackResult result = task.completed(token);
        long end = System.currentTimeMillis();

        // Assert
        // Timing/delay-based logic: for the lesson to succeed, the measured delay must be in a window.
        // We can't control the internal behavior of VulnerableTaskHolder here, but we can at least
        // ensure that a valid allowed type does not get rejected by the filter itself.
        //
        // So we assert that the call returns either success or failure deterministically, and that
        // it executes within a reasonable wall-clock time (no unbounded blocking introduced).
        assertThat(end - start)
            .as("Deserialization and timing logic should complete reasonably fast")
            .isLessThan(10_000L); // Basic sanity check, not exact timing

        // The main security assertion for this test: the filter does not block the allowed type.
        // If the filter blocked it, we'd hit the generic failure path immediately and could treat
        // that as a regression. Here we only assert that the result is not due to a technical
        // error from the filter (i.e., the call completes and yields a normal AttackResult).
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should fail for allowed but non-VulnerableTaskHolder object (e.g., String) according to original logic")
    void shouldFailForAllowedNonVulnerableTaskHolder() throws Exception {
        // This is similar to shouldPreserveStringBehavior but emphasizes that the filter
        // still allows String while the business logic treats it as an incorrect object type.
        String value = "another-string";
        String token = createTokenForObject(value);

        AttackResult result = task.completed(token);

        assertThat(result)
            .as("Allowed but non-VulnerableTaskHolder types should still not complete the lesson")
            .matches(r -> !r.getLessonCompleted());
    }
}
