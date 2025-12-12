package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.owasp.webgoat.container.assignments.AttackResult.Status.SUCCESS;
import static org.owasp.webgoat.container.assignments.AttackResult.Status.FAIL;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for InsecureDeserializationTask focusing on the deserialization hardening (VULN-005).
 */
class InsecureDeserializationTaskSecurityTest {

    @Test
    @DisplayName("VULN-005: Valid VulnerableTaskHolder payload is accepted")
    void validVulnerableTaskHolderPayloadIsAccepted() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = serializeToWebToken(holder);

        AttackResult result = task.completed(token);

        assertThat(result).isNotNull();
        assertThat(result.getStatus())
                .as("VulnerableTaskHolder should be allowed by ObjectInputFilter")
                .isNotEqualTo(FAIL);
    }

    @Test
    @DisplayName("VULN-005: Unexpected object type payload is rejected by filter")
    void unexpectedObjectTypePayloadIsRejected() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        String maliciousObject = "I am not a VulnerableTaskHolder";
        String token = serializeToWebToken(maliciousObject);

        AttackResult result = task.completed(token);

        assertThat(result).isNotNull();
        assertThat(result.getStatus())
                .as("Non-whitelisted type should not be accepted")
                .isEqualTo(FAIL);
    }

    private String serializeToWebToken(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        return base64.replace('+', '-').replace('/', '_');
    }
}
