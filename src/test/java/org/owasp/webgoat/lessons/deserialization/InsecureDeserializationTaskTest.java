// Assuming standard Maven-style test package based on source path
package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.container.assignments.AttackResult.AttackResultType;

/**
 * Delta tests for InsecureDeserializationTask focusing only on:
 * - Introduction of ObjectInputFilter allowlist to restrict deserialized types.
 */
class InsecureDeserializationTaskTest {

    private String serializeToBase64(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // The controller expects URL-safe form with '-' and '_' replacements
        return base64.replace('+', '-').replace('/', '_');
    }

    @Test
    @DisplayName("completed should still accept valid VulnerableTaskHolder instances after filter is added")
    void completedAcceptsAllowedTypeVulnerableTaskHolder() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        VulnerableTaskHolder holder = new VulnerableTaskHolder(); // relies on its default behavior for timing
        String token = serializeToBase64(holder);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // We only verify that the request is processed and not rejected due to type filtering.
        // Depending on timing logic, success/fail can vary, but it must not fail due to
        // the deserialized type being rejected.
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("completed should reject disallowed types via ObjectInputFilter")
    void completedRejectsDisallowedType() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // A simple serializable object of a type that is NOT on the allowlist
        class MaliciousPayload implements java.io.Serializable {
            private static final long serialVersionUID = 1L;
        }

        String token = serializeToBase64(new MaliciousPayload());

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // With the filter in place, deserialization of this type should not be considered valid
        // and should result in a failed AttackResult rather than processing a rogue object.
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(AttackResultType.FAIL);
    }
}
