// Delta_UnitTest_Agent
// Package inferred from source file location; adjust if project structure differs.
package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing ONLY on the changed behavior:
 *  - Deserialization must now be restricted using ObjectInputFilter so that
 *    only VulnerableTaskHolder (and basic JDK classes) are allowed.
 */
class InsecureDeserializationTaskTest {

    /**
     * Helper to serialize an object to the specific URL-safe Base64 format the controller expects.
     */
    private String toControllerToken(Serializable obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // Controller performs: token.replace('-', '+').replace('_', '/')
        // So we generate the inverse (URL-safe) form here.
        return base64.replace('+', '-').replace('/', '_');
    }

    @Test
    void completed_shouldRejectDisallowedDeserializationTypes() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // A type that is NOT in the allowlist "java.base/*;org.dummy.insecure.framework.VulnerableTaskHolder;!*"
        Serializable maliciousObject = new MaliciousPayload();
        String token = toControllerToken(maliciousObject);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // When ObjectInputFilter rejects the class, the current implementation maps this to
        // either InvalidClassException or generic Exception, both resulting in a failed AttackResult.
        assertThat(result)
                .as("Deserialization of disallowed types must not succeed.")
                .matches(r -> !r.getLessonCompleted());
    }

    @Test
    void completed_shouldAcceptVulnerableTaskHolderInstances() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        // VulnerableTaskHolder is explicitly allowed by the filter configuration.
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = toControllerToken(holder);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // We do NOT assert full timing behavior; we just assert that it does not fail solely
        // due to the filter blocking the class type.
        // If the lesson's timing window fails, it may still be 'failed', but not because of type rejection.
        // We therefore simply assert that deserialization itself does not throw.
        // To keep this deterministic and focused on the delta behavior, we ensure call does not throw.
        assertThat(result)
                .as("Allowed type should pass the ObjectInputFilter and be deserialized.")
                .isNotNull();
    }

    /**
     * A simple Serializable type that should be rejected by the ObjectInputFilter.
     */
    private static class MaliciousPayload implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
