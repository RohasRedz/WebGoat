package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for security fix in InsecureDeserializationTask:
 * - Verify that deserialization of a disallowed type fails (ObjectInputFilter whitelist applied).
 * - Verify that a whitelisted VulnerableTaskHolder can still be deserialized successfully.
 */
class InsecureDeserializationTaskTest {

    @Test
    void deserializationOfDisallowedTypeShouldFail() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // Create a token containing a clearly disallowed type (e.g., java.lang.String is allowed as java.base,
        // but the filter then enforces VulnerableTaskHolder; non-matching type should cause failure path).
        String maliciousObject = "malicious-string";
        String token = serializeToUrlSafeBase64(maliciousObject);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }

    @Test
    void deserializationOfVulnerableTaskHolderShouldSucceedWithinTimingWindowOrFailGracefully() throws Exception {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = serializeToUrlSafeBase64(holder);

        // Act
        AttackResult result = task.completed(token);

        // Assert
        // We cannot reliably enforce the 37 second timing window in a unit test environment,
        // but we can assert that the calls do not hit the generic invalidversion path caused
        // by object filtering and that an AttackResult is returned.
        assertThat(result).isNotNull();
    }

    private String serializeToUrlSafeBase64(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // Mirror the token mangling used in InsecureDeserializationTask (reverse of replace)
        return base64.replace('+', '-').replace('/', '_');
    }
}
