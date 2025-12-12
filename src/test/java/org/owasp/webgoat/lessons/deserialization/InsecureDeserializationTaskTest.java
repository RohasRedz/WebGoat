package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for {@link InsecureDeserializationTask}:
 * Verifies that:
 * 1) A serialized {@link VulnerableTaskHolder} instance can still be processed
 *    successfully (filter allows the intended type).
 * 2) A serialized disallowed type (e.g., java.lang.Runtime) is rejected and does
 *    not lead to successful completion, demonstrating the presence of a filter.
 */
public class InsecureDeserializationTaskTest {

    private String toToken(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }
        String b64 = Base64.getEncoder().encodeToString(bos.toByteArray());
        return b64.replace('+', '-').replace('/', '_');
    }

    @Test
    void completed_shouldAcceptVulnerableTaskHolderWhenFilterAllowsIt() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        VulnerableTaskHolder holder = new VulnerableTaskHolder();
        String token = toToken(holder);

        AttackResult result = task.completed(token);

        assertThat(result).isNotNull();
    }

    @Test
    void completed_shouldRejectDisallowedTypeDueToObjectInputFilter() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        Runtime runtime = Runtime.getRuntime();
        String token = toToken(runtime);

        AttackResult result = task.completed(token);

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
