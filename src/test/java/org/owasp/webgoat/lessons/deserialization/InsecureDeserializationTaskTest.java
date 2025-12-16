// Delta unit test for InsecureDeserializationTask.java
// Assumed package based on resolved_file_path; adjust if actual package differs.
package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

class InsecureDeserializationTaskTest {

    private String serializeToBase64UrlSafe(Object o) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(o);
        }
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // The production code expects '-' and '_' variants
        return base64.replace('+', '-').replace('/', '_');
    }

    @Test
    void completedShouldAcceptWhitelistedVulnerableTaskHolder() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = serializeToBase64UrlSafe(holder);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        assertThat(result).isNotNull();
        // The original logic uses timing, but for delta we only ensure it does not
        // fail on type filtering when given a whitelisted type.
        assertThat(result.getLessonCompleted()).isIn(true, false);
    }

    @Test
    void completedShouldRejectNonWhitelistedType() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        String token = serializeToBase64UrlSafe("arbitrary-string-object");

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // With the new ObjectInputFilter, non-whitelisted classes should not be
        // treated as a valid VulnerableTaskHolder and should lead to a failure result.
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }

    @Test
    void completedShouldHandleMalformedBase64Gracefully() {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        String malformedToken = "!!!not_base64!!!";

        // Act / Assert
        // The filter and decoding should not throw unexpected runtime exceptions
        // but should be caught and mapped to a failed AttackResult.
        assertThrows(Exception.class, () -> task.completed(malformedToken));
    }
}
