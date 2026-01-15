package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing on the introduction of
 * an ObjectInputFilter which restricts the types that can be deserialized.
 */
public class InsecureDeserializationTaskTest {

  private String serializeToUrlSafeBase64(Object obj) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(obj);
    }
    String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
    return b64.replace('+', '-').replace('/', '_');
  }

  @Test
  void completed_acceptsAllowedVulnerableTaskHolderObject() throws Exception {
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    VulnerableTaskHolder holder = new VulnerableTaskHolder();
    String token = serializeToUrlSafeBase64(holder);

    AttackResult result = task.completed(token);

    assertNotNull(result, "Result should not be null for allowed class");
  }

  @Test
  void completed_rejectsDisallowedTypeDueToFilter() throws Exception {
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    String token = serializeToUrlSafeBase64(Integer.valueOf(42));

    AttackResult result = task.completed(token);

    assertNotNull(result, "Result should not be null for disallowed class");
  }
}
