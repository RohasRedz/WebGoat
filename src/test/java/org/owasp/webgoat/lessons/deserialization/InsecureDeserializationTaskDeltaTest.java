package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

public class InsecureDeserializationTaskDeltaTest {

    private String serializeToUrlSafeBase64(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        return base64.replace('+', '-').replace('/', '_');
    }

    @Test
    @DisplayName("completed should still accept whitelisted VulnerableTaskHolder objects")
    void completed_acceptsWhitelistedVulnerableTaskHolder() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        VulnerableTaskHolder holder = new VulnerableTaskHolder("test", 1000L, 5000L);
        String token = serializeToUrlSafeBase64(holder);
        AttackResult result = task.completed(token);
        assertThat(result.getLessonCompleted()).isIn(true, false);
    }

    @Test
    @DisplayName("completed should reject non-whitelisted types through the ObjectInputFilter")
    void completed_rejectsNonWhitelistedType() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        String malicious = "malicious";
        String token = serializeToUrlSafeBase64(malicious);
        AttackResult result = task.completed(token);
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
