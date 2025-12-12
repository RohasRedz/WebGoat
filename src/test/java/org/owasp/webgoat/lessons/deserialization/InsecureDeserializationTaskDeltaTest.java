package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.util.Base64;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class InsecureDeserializationTaskDeltaTest {

    @Test
    @DisplayName("InsecureDeserializationTask should reject non-allowlisted deserialized types")
    void insecureDeserializationShouldRejectNonAllowlistedTypes() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        byte[] serialized;
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
            oos.writeObject(Integer.valueOf(42));
            oos.flush();
            serialized = baos.toByteArray();
        }

        String b64 = Base64.getEncoder().encodeToString(serialized);
        String token = b64.replace('+', '-').replace('/', '_');

        var result = task.completed(token);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("ObjectInputStream should be configured with an allowlist ObjectInputFilter")
    void objectInputStreamShouldHaveAllowlistFilter() throws Exception {
        VulnerableTaskHolder holder = org.mockito.Mockito.mock(VulnerableTaskHolder.class);
        byte[] serialized;
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
            oos.writeObject(holder);
            oos.flush();
            serialized = baos.toByteArray();
        }

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialized));

        ObjectInputFilter filter = ObjectInputFilter.Config.createSerializationFilter(
                "org.dummy.insecure.framework.VulnerableTaskHolder;java.lang.String;!*");
        ois.setObjectInputFilter(filter);

        assertThat(ois.getObjectInputFilter()).isNotNull();

        Object read = ois.readObject();
        assertThat(read).isInstanceOf(VulnerableTaskHolder.class);
    }
}
