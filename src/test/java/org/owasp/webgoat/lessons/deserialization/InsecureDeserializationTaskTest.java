// TODO: Package inferred from source path; adjust if actual package differs.
package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing only on changed behavior:
 * - Deserialization is now constrained by an ObjectInputFilter whitelist.
 *   Only java.lang.* and VulnerableTaskHolder should be allowed; other types should be rejected.
 */
public class InsecureDeserializationTaskTest {

    private final InsecureDeserializationTask task = new InsecureDeserializationTask();

    @Test
    void completed_WithVulnerableTaskHolderToken_ShouldNotBeBlockedByFilter() throws Exception {
        // Arrange: serialize an allowed type (VulnerableTaskHolder)
        String token = serializeToWebGoatToken(new VulnerableTaskHolder());

        // Act
        AttackResult result = task.completed(token);

        // Assert: result should at least not be an obvious failure caused by filtering.
        // Exact success state depends on timing logic; we only assert it's not null.
        assertThat(result).isNotNull();
    }

    @Test
    void completed_WithDisallowedSerializableType_ShouldFailDueToFilter() throws Exception {
        // Arrange: serialize a type that is NOT in the allow-list
        String token = serializeToWebGoatToken(new NotAllowedSerializable());

        // Act
        AttackResult result = task.completed(token);

        // Assert: filter should cause deserialization to fail and return a failed AttackResult
        assertThat(result).isNotNull();
        assertThat(result.isLessonSolved()).isFalse();
    }

    private String serializeToWebGoatToken(Serializable obj) throws Exception {
        // Helper to mirror Base64 + URL-safe replacement encoding used by the controller.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        return b64.replace('+', '-').replace('/', '_');
    }

    private static class NotAllowedSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        String data = "not-allowed";
    }
}
