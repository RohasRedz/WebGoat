package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

class InsecureDeserializationTaskTest {

    @Test
    void completedShouldAcceptVulnerableTaskHolder() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        String token = serializeToWebToken(new VulnerableTaskHolder());
        AttackResult result = task.completed(token);
        assertNotNull(result, "Result must not be null for valid VulnerableTaskHolder token");
    }

    @Test
    void completedShouldRejectOtherClasses() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        String token = serializeToWebToken("malicious-string");
        AttackResult result = task.completed(token);
        assertNotNull(result, "Result must not be null for invalid token");
        assertFalse(result.getLessonCompleted(), "Lesson should not be completed for non-VulnerableTaskHolder objects");
    }

    private String serializeToWebToken(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }
        String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
        return base64.replace('+', '-').replace('/', '_');
    }
}
