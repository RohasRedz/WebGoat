package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for {@link InsecureDeserializationTask} focusing only on:
 * - Only allowed classes (VulnerableTaskHolder, String) can be deserialized.
 * - Basic expected behavior for a valid VulnerableTaskHolder token remains correct.
 */
class InsecureDeserializationTaskTest {

    private String toUrlSafeBase64(byte[] bytes) {
        String b64 = Base64.getEncoder().encodeToString(bytes);
        return b64.replace('+', '-').replace('/', '_');
    }

    @Test
    void shouldSuccessfullyHandleValidVulnerableTaskHolderTokenWithinExpectedDelayRange() throws IOException {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        VulnerableTaskHolder holder = new VulnerableTaskHolder(3000); // within [3000, 7000] ms

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(holder);
        }
        String token = toUrlSafeBase64(bos.toByteArray());

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // The deserialization filter must allow VulnerableTaskHolder and the timing heuristic
        // must consider this a success case.
        assertThat(result.getLessonCompleted()).isTrue();
    }

    @Test
    void shouldRejectDisallowedClassByDeserializationFilter() throws IOException {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        // Serialize an object of a class that is NOT in the allowed list
        Object maliciousObject = new java.util.Date();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(maliciousObject);
        }
        String token = toUrlSafeBase64(bos.toByteArray());

        // Act & Assert
        // The filter should prevent successful deserialization of this type.
        // Depending on JDK behavior, this may result in an InvalidClassException or a generic
        // exception; in either case, the endpoint should translate it to a failed AttackResult.
        AttackResult result = task.completed(token);
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
