package org.owasp.webgoat.lessons.deserialization;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Delta tests for InsecureDeserializationTask focusing on the vulnerability fix:
 * constraining deserialization via ObjectInputFilter to allowed classes only.
 */
class InsecureDeserializationTaskTest {

    /**
     * Verifies that an allowed class instance can still be deserialized successfully,
     * ensuring that legitimate behavior is preserved.
     */
    @Test
    void deserializeAllowedClassSucceeds() throws IOException, ClassNotFoundException {
        InsecureDeserializationTask.AllowedClass allowed = new InsecureDeserializationTask.AllowedClass();
        byte[] serialized = serialize(allowed);

        InsecureDeserializationTask task = new InsecureDeserializationTask();

        Object result = task.deserialize(serialized);

        assertNotNull(result, "Deserialization result should not be null for allowed class");
        assertTrue(result instanceof InsecureDeserializationTask.AllowedClass,
                "Result should be instance of allowed class");
    }

    /**
     * Verifies that a non-whitelisted class is rejected by the filter.
     * Expected behavior: either a ClassNotFoundException, IOException, or some other checked exception path.
     * The exact exception type may vary by JDK implementation; we primarily assert that it does not succeed.
     */
    @Test
    void deserializeDisallowedClassFails() throws IOException {
        Disallowed disallowed = new Disallowed();
        byte[] serialized = serialize(disallowed);

        InsecureDeserializationTask task = new InsecureDeserializationTask();

        assertThrows(Exception.class, () -> task.deserialize(serialized),
                "Deserializing a non-allowed class should fail due to ObjectInputFilter restrictions");
    }

    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    /**
     * Helper class not referenced by the filter pattern and therefore should be rejected.
     */
    private static class Disallowed implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
    }
}
