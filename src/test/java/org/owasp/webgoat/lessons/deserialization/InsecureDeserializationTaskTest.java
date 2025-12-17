/* Delta tests for InsecureDeserializationTask focusing on ObjectInputFilter behavior. */
package org.owasp.webgoat.lessons.deserialization;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class InsecureDeserializationTaskTest {

    private final InsecureDeserializationTask task = new InsecureDeserializationTask();

    private String toUrlSafeBase64(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }
        String standardB64 = Base64.getEncoder().encodeToString(bos.toByteArray());
        return standardB64.replace('+', '-').replace('/', '_');
    }

    @Test
    void completed_shouldAcceptVulnerableTaskHolderWhenFilterAllowsIt() throws Exception {
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = toUrlSafeBase64(holder);

        AttackResult result = task.completed(token);

        assertThat(result).isNotNull();
    }

    @Test
    void completed_shouldRejectStringObject() throws Exception {
        String payload = "Just a String";
        String token = toUrlSafeBase64(payload);

        AttackResult result = task.completed(token);

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }

    @Test
    void completed_shouldFailForDisallowedType() throws Exception {
        Runtime malicious = Runtime.getRuntime();
        String token = toUrlSafeBase64(malicious);

        AttackResult result = task.completed(token);

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
