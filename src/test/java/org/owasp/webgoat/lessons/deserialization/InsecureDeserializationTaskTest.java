package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

public class InsecureDeserializationTaskTest {

    private final InsecureDeserializationTask endpoint = new InsecureDeserializationTask();

    private String serializeToBase64(Object obj) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bout)) {
            oos.writeObject(obj);
        }
        String base64 = Base64.getEncoder().encodeToString(bout.toByteArray());
        return base64.replace('+', '-').replace('/', '_');
    }

    @Test
    void completed_shouldAcceptAllowedClassVulnerableTaskHolder() throws Exception {
        VulnerableTaskHolder holder = new VulnerableTaskHolder("task");
        String token = serializeToBase64(holder);

        AttackResult result = endpoint.completed(token);

        assertThat(result.getFeedback()).doesNotContain("wrongobject");
    }

    @Test
    void completed_shouldRejectDisallowedClass() throws Exception {
        Integer malicious = 42;
        String token = serializeToBase64(malicious);

        AttackResult result = endpoint.completed(token);

        assertThat(result.getFeedback()).contains("insecure-deserialization.invalidversion");
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
