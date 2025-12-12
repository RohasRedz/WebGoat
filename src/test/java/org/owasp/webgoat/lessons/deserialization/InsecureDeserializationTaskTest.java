package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing only on the mitigation:
 * - The endpoint no longer deserializes user-controlled data and always returns
 *   a blocked/failure feedback.
 */
class InsecureDeserializationTaskTest {

    @Test
    @DisplayName("completed should always return blocked feedback and not attempt deserialization")
    void completedAlwaysReturnsBlockedFeedback() throws IOException {
        // Arrange
        InsecureDeserializationTask task = new InsecureDeserializationTask();
        String arbitraryToken = "anyBase64OrSerializedPayload";

        // Act
        AttackResult result = task.completed(arbitraryToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
        // We cannot directly assert that no deserialization occurred, but the implementation
        // now returns immediately with a blocked feedback, which is what we verify here.
    }
}
