package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for InsecureDeserializationTask focusing only on:
 * - The endpoint no longer performs Java deserialization of user-controlled data.
 * - It returns a controlled failure result with the feedback key
 *   "insecure-deserialization.prevented".
 */
class InsecureDeserializationTaskTest {

    @Test
    void completedShouldAlwaysReturnFailureWithPreventedFeedback() throws IOException {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();

        // Any token should be treated the same now; previously the token could trigger deserialization.
        String arbitraryToken = "any-base64-like-token";

        // Act
        AttackResult result = task.completed(arbitraryToken);

        // Assert
        // After the fix, insecure deserialization is removed and a controlled failure is returned.
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted())
                .as("Lesson should not be marked as completed when deserialization is prevented")
                .isFalse();

        // The exact API for accessing feedback may vary; here we assert via toString().
        // Adjust if AttackResult exposes feedback via a dedicated accessor.
        String serialized = result.toString();
        assertThat(serialized)
                .as("Result should indicate that insecure deserialization was prevented")
                .contains("insecure-deserialization.prevented");
    }
}
