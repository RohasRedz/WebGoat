package org.owasp.webgoat.lessons.deserialization;

// TODO: Package is inferred from the updated Java file; adjust if the actual package differs.

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;

/**
 * Delta unit tests for {@link InsecureDeserializationTask}.
 *
 * <p>Focus: validate the new deserialization filter behavior introduced to mitigate insecure
 * deserialization of user-controlled data. We only test behavior that changed:
 *
 * <ul>
 *   <li>Allowed types (VulnerableTaskHolder and String) can be deserialized without the filter
 *       blocking them.
 *   <li>Disallowed types are rejected by the filter, causing the method to fail rather than
 *       successfully processing an arbitrary object.
 * </ul>
 */
public class InsecureDeserializationTaskSecurityTest {

  /**
   * Helper method to serialize an arbitrary object to a WebGoat-style URL-safe Base64 token.
   *
   * <p>The production code expects the token format:
   *
   * <pre>
   * token = Base64-encoded bytes
   *   with '+' replaced by '-' and '/' replaced by '_'
   * </pre>
   */
  private String toWebGoatToken(Object obj) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(obj);
    }
    String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
    // Apply the same URL-safe mangling that the controller reverses
    return base64.replace('+', '-').replace('/', '_');
  }

  /**
   * Verifies that the new deserialization filter allows the expected, whitelisted type
   * {@link VulnerableTaskHolder} to be deserialized without being rejected by the filter.
   *
   * <p>We don't assert on the specific lesson semantics (timing, success vs failure feedback),
   * only that no unexpected exception is thrown by the filter itself while processing this allowed
   * type.
   */
  @Test
  void deserialization_allowsWhitelistedVulnerableTaskHolder() throws Exception {
    InsecureDeserializationTask controller = new InsecureDeserializationTask();

    // Create a simple VulnerableTaskHolder instance; details are unimportant for the filter
    VulnerableTaskHolder holder = new VulnerableTaskHolder();
    String token = toWebGoatToken(holder);

    // Assert that the method does not throw because of the filter when handling a whitelisted type.
    assertDoesNotThrow(() -> controller.completed(token));
  }

  /**
   * Verifies that the deserialization filter allows {@link String}, which is explicitly
   * whitelisted in the filter expression.
   *
   * <p>The lesson logic itself will likely consider this a failure and return an error AttackResult,
   * but from the security perspective we only care that the filter does not block this allowed
   * type. Hence, we assert only that no unexpected exception is thrown by the filter path.
   */
  @Test
  void deserialization_allowsWhitelistedString() throws Exception {
    InsecureDeserializationTask controller = new InsecureDeserializationTask();

    String token = toWebGoatToken("test-string");

    // The filter should not throw when deserializing a whitelisted String instance.
    assertDoesNotThrow(() -> controller.completed(token));
  }

  /**
   * Verifies that the new deserialization filter blocks deserialization of disallowed classes.
   *
   * <p>We serialize a benign but non-whitelisted Serializable type. The filter is configured as:
   *
   * <pre>
   * "org.dummy.insecure.framework.VulnerableTaskHolder;java.lang.String;!*"
   * </pre>
   *
   * which should reject this type and cause the controller to throw (either via
   * InvalidClassException or a generic exception path). The important delta behavior is that such
   * objects are no longer silently deserialized and processed.
   */
  @Test
  void deserialization_blocksNonWhitelistedType() throws Exception {
    InsecureDeserializationTask controller = new InsecureDeserializationTask();

    // A simple arbitrary Serializable type that is not on the allowlist
    class EvilPayload implements Serializable {
      private static final long serialVersionUID = 1L;
      String value = "evil";
    }

    String token = toWebGoatToken(new EvilPayload());

    // The filter or subsequent validation should prevent successful processing;
    // we expect an exception to be thrown from the completed(...) method.
    assertThrows(Exception.class, () -> controller.completed(token));
  }
}
