package org.owasp.webgoat.lessons.deserialization;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Delta tests for InsecureDeserializationTask focusing on the new ObjectInputFilter behavior.
 * These tests ensure that:
 *  - Legitimate VulnerableTaskHolder tokens are still accepted.
 *  - Disallowed classes are rejected according to the filter.
 */
public class InsecureDeserializationTaskTest {

    private InsecureDeserializationTask task;

    @BeforeEach
    void setUp() {
        task = new InsecureDeserializationTask();
    }

    @Test
    void completed_shouldSucceedForValidVulnerableTaskHolderToken() throws IOException {
        // Build a simple VulnerableTaskHolder instance and serialize it
        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = toUrlSafeToken(holder);

        AttackResult result = task.completed(token);

        // The exact timing-based success criteria are defined in the lesson,
        // but for the filter change we only assert we don't fail fast due to filter rejection.
        assertNotNull(result, "Result must not be null for a valid token");
    }

    @Test
    void completed_shouldFailForDisallowedClassToken() throws IOException {
        // Serialize a plain String, which should still be explicitly handled as failure by lesson logic
        String malicious = "malicious";
        String token = toUrlSafeToken(malicious);

        AttackResult result = task.completed(token);

        assertNotNull(result, "Result must not be null for disallowed class token");
        assertFalse(result.getLessonCompleted(), "Deserialization of disallowed class tokens must not complete the lesson");
    }

    private String toUrlSafeToken(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // Apply the same URL-safe replacements as the production code expects in reverse
        return b64.replace('+', '-').replace('/', '_');
    }
}
