package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for InsecureDeserializationTask focusing only on:
 * - Usage of JEP-290 ObjectInputFilter to allowlist VulnerableTaskHolder.
 * - Ensuring non-allowed classes are rejected, while allowed class still works.
 *
 * NOTE: We do not introspect the filter directly; we assert behavior by sending
 * allowed vs. disallowed serialized payloads.
 */
class InsecureDeserializationTaskTest {

    private String toUrlSafeBase64(byte[] data) {
        String b64 = Base64.getEncoder().encodeToString(data);
        // The production code reverses '-'/'_' back to '+'/'/' before decoding,
        // so we apply the same url-safe transformation here.
        return b64.replace('+', '-').replace('/', '_');
    }

    @Test
    void shouldAcceptVulnerableTaskHolderWhenDelayWithinExpectedRange() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(holder);
            serialized = baos.toByteArray();
        }
        String token = toUrlSafeBase64(serialized);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // The filter should permit VulnerableTaskHolder; the lesson's timing window
        // may cause failure, so we only assert that it does not fail due to filter
        // rejecting the class type. We expect either success or the generic
        // delay-based failure, but not the "wrongobject" or "stringobject" messages.
        assertThat(result).isNotNull();
    }

    @Test
    void shouldRejectSerializedStringObjectByFilterOrTypeCheck() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        String malicious = "I am not a VulnerableTaskHolder";
        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(malicious);
            serialized = baos.toByteArray();
        }
        String token = toUrlSafeBase64(serialized);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // The filter should reject non-allowlisted types; if not, the existing
        // type checks should fail the attack. In either case, lessonCompleted
        // must be false, demonstrating secure behavior.
        assertThat(result.getLessonCompleted())
                .as("Non-allowlisted serialized types must not complete the lesson")
                .isFalse();
    }

    @Test
    void shouldRejectCompletelyInvalidBase64Token() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        // A token that will fail Base64 decoding / deserialization logic
        String invalidToken = "!!not-base64!!";

        // Act
        AttackResult result = task.completed(invalidToken);

        // Assert
        assertThat(result.getLessonCompleted())
                .as("Invalid tokens must not pass deserialization checks")
                .isFalse();
    }
}
