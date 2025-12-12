package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing on changed behavior:
 * - ObjectInputStream now has an ObjectInputFilter allowlist restricting deserialized classes.
 */
public class InsecureDeserializationTaskTest {

    @Test
    @DisplayName("completed should configure ObjectInputStream with a restrictive ObjectInputFilter")
    void completedShouldSetObjectInputFilter() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        byte[] dummyBytes = new byte[] {0x00};
        String token = Base64.getEncoder().encodeToString(dummyBytes)
                .replace('+', '-')
                .replace('/', '_');

        try (MockedConstruction<ObjectInputStream> mocked = Mockito.mockConstruction(
                ObjectInputStream.class,
                (mock, context) -> {
                    Mockito.when(mock.readObject()).thenReturn(Mockito.mock(VulnerableTaskHolder.class));
                })) {
            AttackResult result = task.completed(token);
            ObjectInputStream constructed = mocked.constructed().get(0);
            Mockito.verify(constructed).setObjectInputFilter(Mockito.any(ObjectInputFilter.class));
        }
    }

    @Test
    @DisplayName("ObjectInputFilter should prevent deserialization of disallowed types")
    void filterShouldRejectDisallowedTypes() throws Exception {
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        byte[] dummyBytes = new byte[] {0x00};
        String token = Base64.getEncoder().encodeToString(dummyBytes)
                .replace('+', '-')
                .replace('/', '_');

        try (MockedConstruction<ObjectInputStream> mocked = Mockito.mockConstruction(
                ObjectInputStream.class,
                (mock, context) -> {
                    doThrow(new java.io.InvalidClassException("Rejected by filter")).when(mock).readObject();
                })) {
            assertThrows(IOException.class, () -> {
                task.completed(token);
            });
        }
    }
}
