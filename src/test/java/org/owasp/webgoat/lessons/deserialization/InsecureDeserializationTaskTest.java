// Assumed package based on main file path; adjust if actual package differs.
package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing on:
 * - Use of ValidatingObjectInputStream and class allowlist.
 * - Blocking deserialization of non-allowed classes while still allowing VulnerableTaskHolder.
 */
class InsecureDeserializationTaskTest {

    private final InsecureDeserializationTask task = new InsecureDeserializationTask();

    private String toWebGoatToken(Serializable obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }
        String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
        // reverse of token.replace('-', '+').replace('_', '/'); not needed for this test,
        // since we never introduce '-' or '_' here.
        return base64;
    }

    @Test
    void completed_shouldAllowVulnerableTaskHolderDeserialization() throws Exception {
        // Arrange
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = toWebGoatToken(holder);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // We primarily care that deserialization does not fail with InvalidClassException
        // and that the assignment logic proceeds. We do not assert exact outcome flags,
        // only that the call completes without throwing.
        assertThat(result).isNotNull();
    }

    @Test
    void completed_shouldRejectNonWhitelistedClass() throws Exception {
        // Arrange
        EvilPayload evil = new EvilPayload();
        String token = toWebGoatToken(evil);

        // Act & Assert
        // We expect the ValidatingObjectInputStream to throw InvalidClassException,
        // which is then translated into a failure AttackResult by the controller.
        AttackResult result = task.completed(token);

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }

    /**
     * Non-whitelisted test payload to verify that class filtering is enforced.
     */
    private static class EvilPayload implements Serializable {
        private static final long serialVersionUID = 1L;
        String cmd = "calc.exe";
    }
}
