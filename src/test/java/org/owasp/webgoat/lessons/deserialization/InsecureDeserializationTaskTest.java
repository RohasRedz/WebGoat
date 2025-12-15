package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for InsecureDeserializationTask focusing only on:
 * - Introduction of ObjectInputFilter allowlisting
 * - Ensuring non-allowed types are rejected while allowed type still works
 */
public class InsecureDeserializationTaskTest {

    /**
     * Utility to Base64-url encode an arbitrary Serializable object in the same way
     * the controller expects it (reverse of token.replace('-', '+').replace('_', '/')).
     */
    private String toUrlSafeBase64(Object value) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(value);
        }
        String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // reverse of token.replace('-', '+').replace('_', '/')
        return b64.replace('+', '-').replace('/', '_');
    }

    @Test
    @DisplayName("completed should still accept VulnerableTaskHolder instances after adding ObjectInputFilter")
    void completed_allowsVulnerableTaskHolder() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = toUrlSafeBase64(holder);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // Even with the allowlist filter, the expected type must still be accepted
        assertTrue(result.getLessonCompleted(),
                "VulnerableTaskHolder should remain deserializable after introducing ObjectInputFilter allowlisting");
    }

    @Test
    @DisplayName("completed should reject disallowed object types via ObjectInputFilter")
    void completed_rejectsDisallowedTypes() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // Use a harmless but disallowed type (e.g., java.lang.Integer) to avoid any environment-specific gadgets
        Integer disallowedObject = Integer.valueOf(42);
        String token = toUrlSafeBase64(disallowedObject);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // The filter pattern allows only java.lang.String and VulnerableTaskHolder; other types must fail
        assertFalse(result.getLessonCompleted(),
                "Disallowed object types must not complete the lesson after ObjectInputFilter allowlisting");
    }
}
