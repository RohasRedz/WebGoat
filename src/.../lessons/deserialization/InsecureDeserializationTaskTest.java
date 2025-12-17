// TODO: Adjust package based on the actual source package declaration.
package org.owasp.webgoat.lessons.deserialization;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Delta tests for InsecureDeserializationTask focusing on the insecure-deserialization fix.
 *
 * The updated code:
 * - Introduces a whitelist of allowed classes (e.g., String, ArrayList).
 * - Applies ObjectInputFilter to reject deserialization of unapproved types.
 *
 * These tests verify:
 * - Allowed types (String, ArrayList) can be deserialized successfully.
 * - Disallowed/custom types are rejected during deserialization.
 */
class InsecureDeserializationTaskTest {

    private final InsecureDeserializationTask task = new InsecureDeserializationTask();

    @Test
    void safeDeserializeShouldAllowWhitelistedTypesLikeString() throws Exception {
        // Arrange
        String original = "whitelisted-string";
        byte[] serialized = serialize(original);

        // Act
        Object result = task.safeDeserialize(serialized);

        // Assert
        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo(original);
    }

    @Test
    void safeDeserializeShouldAllowWhitelistedCollectionTypeLikeArrayList() throws Exception {
        // Arrange
        ArrayList<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        byte[] serialized = serialize(list);

        // Act
        Object result = task.safeDeserialize(serialized);

        // Assert
        assertThat(result).isInstanceOf(ArrayList.class);
        @SuppressWarnings("unchecked")
        ArrayList<String> deserialized = (ArrayList<String>) result;
        assertThat(deserialized).containsExactly("one", "two");
    }

    @Test
    void safeDeserializeShouldRejectNonWhitelistedCustomType() throws Exception {
        // Arrange
        MaliciousLikeObject malicious = new MaliciousLikeObject("payload");
        byte[] serialized = serialize(malicious);

        // Act & Assert
        // Depending on JDK implementation, rejection may result in InvalidClassException,
        // ObjectInputFilter$FilterInfoStatus REJECTED, or a subclass of IOException.
        assertThrows(IOException.class, () -> task.safeDeserialize(serialized));
    }

    /**
     * Helper method to serialize an object to a byte array.
     */
    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    /**
     * A simple Serializable custom type that should NOT appear in the allowlist and
     * therefore must be rejected by the ObjectInputFilter in safeDeserialize.
     */
    private static class MaliciousLikeObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String value;

        MaliciousLikeObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "MaliciousLikeObject{" +
                    "value='" + value + '\'' +
                    '}';
        }
    }
}
