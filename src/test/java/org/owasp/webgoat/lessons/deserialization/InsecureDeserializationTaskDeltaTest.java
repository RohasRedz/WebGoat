package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing only on changed behavior:
 *
 * 1) The insecure Java deserialization via ObjectInputStream/readObject has been removed and
 *    the token is treated purely as Base64-encoded data (no object graph deserialization).
 *    NOTE: This is validated indirectly via behavioral checks, since low-level implementation
 *    details are not directly observable from the public API.
 *
 * 2) Valid integer tokens within the intended delay window still result in a successful
 *    AttackResult.
 *
 * 3) Malformed or non-integer tokens result in a failed AttackResult with appropriate feedback.
 */
class InsecureDeserializationTaskDeltaTest {

  private final InsecureDeserializationTask insecureDeserializationTask =
      new InsecureDeserializationTask();

  @Test
  @DisplayName(
      "Valid integer token in acceptable delay range (e.g., 5000 ms) should yield success AttackResult")
  void validIntegerTokenWithinRangeShouldSucceed() throws Exception {
    // Arrange
    // The fixed implementation interprets the decoded token as an integer delay in milliseconds
    // and uses Thread.sleep(...) to simulate execution time. The lesson considers a delay between
    // 3000 and 7000 ms as success. We choose a mid-range value (5000).
    String delayString = "5000";
    String token = base64UrlEncode(delayString);

    // Act
    AttackResult result = insecureDeserializationTask.completed(token);

    // Assert
    assertThat(result)
        .as("AttackResult for valid delay must indicate lesson completion")
        .extracting(AttackResult::getLessonCompleted)
        .isEqualTo(true);
  }

  @Test
  @DisplayName(
      "Integer token below minimum delay threshold (e.g., 1000 ms) should yield failed AttackResult")
  void integerTokenBelowRangeShouldFail() throws Exception {
    // Arrange
    // Value clearly below the accepted lower bound (3000 ms), e.g., 1000 ms.
    String delayString = "1000";
    String token = base64UrlEncode(delayString);

    // Act
    AttackResult result = insecureDeserializationTask.completed(token);

    // Assert
    assertThat(result)
        .as("Delay below lower threshold must not complete the lesson")
        .extracting(AttackResult::getLessonCompleted)
        .isEqualTo(false);
  }

  @Test
  @DisplayName(
      "Integer token above maximum delay threshold (e.g., 9000 ms) should yield failed AttackResult")
  void integerTokenAboveRangeShouldFail() throws Exception {
    // Arrange
    // Value clearly above the accepted upper bound (7000 ms), e.g., 9000 ms.
    String delayString = "9000";
    String token = base64UrlEncode(delayString);

    // Act
    AttackResult result = insecureDeserializationTask.completed(token);

    // Assert
    assertThat(result)
        .as("Delay above upper threshold must not complete the lesson")
        .extracting(AttackResult::getLessonCompleted)
        .isEqualTo(false);
  }

  @Test
  @DisplayName(
      "Non-integer token should fail with 'invalidtokenformat' feedback instead of deserializing arbitrary objects")
  void nonIntegerTokenShouldFailWithInvalidTokenFormatFeedback() throws Exception {
    // Arrange
    // This simulates user-controlled input which cannot be parsed as an integer.
    // In the old implementation, this would result in various deserialization paths.
    // In the fixed version, it must result in a safe failure with a specific feedback key.
    String nonInteger = "not-an-integer";
    String token = base64UrlEncode(nonInteger);

    // Act
    AttackResult result = insecureDeserializationTask.completed(token);

    // Assert
    assertThat(result.getLessonCompleted())
        .as("Non-integer token must not complete the lesson")
        .isFalse();
    assertThat(result.getFeedback())
        .as("Non-integer token must be treated as invalid token format")
        .contains("invalidtokenformat");
  }

  @Test
  @DisplayName(
      "Malformed Base64 token should fail with 'malformedbase64' feedback and never trigger deserialization")
  void malformedBase64TokenShouldFailWithMalformedBase64Feedback() throws Exception {
    // Arrange
    // Intentionally provide a string that is not valid Base64 to trigger the Base64 decode error
    // path in the fixed code.
    String malformedBase64 = "%%%NOT_BASE64%%%";

    // Act
    AttackResult result = insecureDeserializationTask.completed(malformedBase64);

    // Assert
    assertThat(result.getLessonCompleted())
        .as("Malformed Base64 token must not complete the lesson")
        .isFalse();
    assertThat(result.getFeedback())
        .as("Malformed Base64 must be reported with specific feedback")
        .contains("malformedbase64");
  }

  /**
   * Helper to emulate the token normalization logic in the controller: input is treated as a
   * Base64-url-safe encoding (i.e., '+' -> '-', '/' -> '_').
   */
  private String base64UrlEncode(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    String base64 = Base64.getEncoder().encodeToString(bytes);
    // The controller decodes by replacing '-' with '+' and '_' with '/', so we do the inverse here.
    return base64.replace('+', '-').replace('/', '_');
  }
}
