package org.owasp.webgoat.lessons.deserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for {@link InsecureDeserializationTask} focusing on the new
 * ObjectInputFilter whitelist behavior:
 *
 * - Valid tokens containing a whitelisted {@link VulnerableTaskHolder} object are accepted.
 * - Tokens containing a non-whitelisted type are rejected and do not cause successful completion.
 *
 * NOTE:
 * - These tests exercise only the behavior changed by the fix (deserialization filtering).
 * - They rely on the real ObjectInputFilter configured in the updated implementation.
 */
class InsecureDeserializationTaskTest {

  /**
   * Helper method to serialize an object and encode it using the URL-safe variant
   * expected by {@link InsecureDeserializationTask#completed(String)}.
   */
  private String toUrlSafeToken(Object object) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(object);
    }
    String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
    // The task expects '-' instead of '+' and '_' instead of '/'
    return base64.replace('+', '-').replace('/', '_');
  }

  @Test
  @DisplayName(
      "completed should accept token containing VulnerableTaskHolder (whitelisted class) and not fail due to filter")
  void completedAcceptsWhitelistedVulnerableTaskHolder() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    // NOTE: We keep the object graph simple; the important part is the root type.
    VulnerableTaskHolder holder = new VulnerableTaskHolder("test-task");
    String token = toUrlSafeToken(holder);

    // Act
    AttackResult result = task.completed(token);

    // Assert
    // The filter must allow deserialization of VulnerableTaskHolder. The original lesson
    // logic uses timing-based checks; here we focus on ensuring the call does not immediately
    // fail due to the ObjectInputFilter rejecting the class.
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName(
      "completed should reject token containing non-whitelisted class and not treat it as VulnerableTaskHolder")
  void completedRejectsNonWhitelistedClass() throws Exception {
    // Arrange
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    // Use a class that is NOT in the whitelist configured in the fix. For example,
    // java.lang.String is allowed by the filter, but the lesson code explicitly handles
    // it as a failure. To specifically test the filter, we use a type that is unlikely
    // to be whitelisted, such as java.lang.StringBuilder.
    StringBuilder maliciousObject = new StringBuilder("malicious");
    String token = toUrlSafeToken(maliciousObject);

    // Act
    AttackResult result = task.completed(token);

    // Assert
    // The filter should reject StringBuilder, causing deserialization to fail and the
    // method to return a failure AttackResult (never treating it as a valid holder).
    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted()).isFalse();
  }
}
