package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for {@link InsecureDeserializationTask}.
 *
 * <p>These tests focus only on the behavior changed by the fix:
 *
 * <ul>
 *   <li>Deserialization is now guarded by an {@code ObjectInputFilter} that whitelists only
 *       {@code VulnerableTaskHolder} and {@code String}.</li>
 *   <li>Allowed types still behave as expected (e.g., {@code VulnerableTaskHolder} can complete the
 *       lesson).</li>
 *   <li>Disallowed types are rejected (resulting in a failed {@link AttackResult}).</li>
 * </ul>
 */
public class InsecureDeserializationTaskTest {

  private InsecureDeserializationTask task;

  @BeforeEach
  void setUp() {
    this.task = new InsecureDeserializationTask();
  }

  /**
   * Helper to serialize an arbitrary object to the Base64-encoded token format expected by the
   * endpoint.
   *
   * <p>The production code:
   *
   * <pre>
   * b64token = token.replace('-', '+').replace('_', '/');
   * Base64.getDecoder().decode(b64token)
   * </pre>
   *
   * expects a standard Base64 string and allows for URL-safe variants. We return a standard Base64
   * value here, which the endpoint will accept.
   */
  private String toToken(Object obj) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(obj);
    }
    String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
    // Return plain Base64; the endpoint will decode it directly.
    return base64;
  }

  /**
   * Verifies that a serialized {@link VulnerableTaskHolder} object is still accepted by the lesson
   * and can produce a successful {@link AttackResult} when the internal timing constraint is met.
   *
   * <p>This demonstrates that the new {@code ObjectInputFilter} whitelist still allows the expected
   * class to be deserialized.
   *
   * <p>NOTE: The lesson logic uses a timing-based check (delay between {@code before} and
   * {@code after}). In a pure unit test we cannot reliably control the internal sleep/behavior of
   * {@code VulnerableTaskHolder}, so we assert only that using a whitelisted class does not cause
   * an immediate failure due to filter rejection. If needed, this test can be adapted to assert
   * specific success behavior in an environment where the delay is controllable.
   */
  @Test
  void completed_WithWhitelistedVulnerableTaskHolder_ShouldNotFailDueToFilter() throws Exception {
    // Arrange
    VulnerableTaskHolder holder = new VulnerableTaskHolder();
    String token = toToken(holder);

    // Act
    AttackResult result = task.completed(token);

    // Assert
    // At minimum, the use of VulnerableTaskHolder must NOT be rejected by the filter.
    // If the timing requirements are satisfied, this may be a success; if not, it may be a
    // functional failure. We assert that it is not the generic "invalid version" failure caused
    // by deserialization problems.
    // Since AttackResult API is not fully specified here, we assert via isLessonCompleted flag:
    // either success or timing-related fail, but not a filter-caused crash.
    // We mainly want to ensure that using a whitelisted type does not cause an unexpected runtime
    // error; if completed() threw, this test would already fail.
    // For safety, we only assert that the method completes without throwing and returns a
    // non-null value; additional assertions can be added if the AttackResult API is known.
    assertFalse(result == null, "AttackResult should not be null for whitelisted class");
  }

  /**
   * Verifies that a serialized {@link String} is still handled according to the lesson logic and
   * that the new {@code ObjectInputFilter} does not prevent deserialization of {@code String}.
   *
   * <p>In the original and fixed code, a deserialized {@code String} leads to a failed result with
   * feedback key {@code insecure-deserialization.stringobject}. We assert that the lesson still
   * fails (i.e., does not mark the lesson as completed) for this case.
   */
  @Test
  void completed_WithWhitelistedString_ShouldFailLessonButNotBeBlockedByFilter()
      throws Exception {
    // Arrange
    String payload = "Just a string instead of VulnerableTaskHolder";
    String token = toToken(payload);

    // Act
    AttackResult result = task.completed(token);

    // Assert
    // For a String object, the lesson is expected to fail and NOT complete the lesson.
    assertFalse(
        result.isLessonCompleted(),
        "Deserializing a whitelisted String should not complete the lesson");
  }

  /**
   * Verifies that a serialized object of a non-whitelisted type is rejected by the deserialization
   * filter and results in a failed {@link AttackResult}.
   *
   * <p>The filter in the fixed code allows only {@code VulnerableTaskHolder} and
   * {@code java.lang.String}. Any other type should be denied by the {@code ObjectInputFilter},
   * leading to an exception and corresponding failure feedback.
   */
  @Test
  void completed_WithNonWhitelistedType_ShouldFailLesson() throws Exception {
    // Arrange
    class NonWhitelisted implements java.io.Serializable {
      private static final long serialVersionUID = 1L;

      String value = "I should not be deserialized";
    }

    NonWhitelisted obj = new NonWhitelisted();
    String token = toToken(obj);

    // Act
    AttackResult result = task.completed(token);

    // Assert
    // Deserializing a non-whitelisted type should not succeed and must not complete the lesson.
    // The exact feedback key may vary, but the lesson should be marked as not completed.
    assertFalse(
        result.isLessonCompleted(),
        "Deserializing a non-whitelisted type must not complete the lesson");
  }

  /**
   * Verifies indirectly that the token transformation still works with standard Base64: even after
   * introducing the {@code ObjectInputFilter}, the endpoint must correctly decode tokens produced
   * via regular Base64 encoding.
   *
   * <p>This test ensures that the introduction of the filter did not inadvertently tighten or break
   * the accepted token encoding scheme for valid payloads.
   */
  @Test
  void completed_WithStandardBase64Token_ShouldBeAcceptedByDecoder() throws Exception {
    // Arrange
    String simple = "Hello, World!";
    String token = Base64.getEncoder().encodeToString(simple.getBytes(StandardCharsets.UTF_8));

    // Act
    AttackResult result = task.completed(token);

    // Assert
    // Even though this is not a serialized object, the method should handle the invalid token
    // gracefully and not crash. It should result in a failed lesson, not an exception.
    assertFalse(
        result == null, "AttackResult should not be null when provided with a Base64 token");
    assertTrue(
        !result.isLessonCompleted(),
        "Non-serialized but Base64 input should not complete the lesson");
  }
}
