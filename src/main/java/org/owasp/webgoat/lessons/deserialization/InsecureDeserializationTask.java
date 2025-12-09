/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.deserialization;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints({
  "insecure-deserialization.hints.1",
  "insecure-deserialization.hints.2",
  "insecure-deserialization.hints.3"
})
public class InsecureDeserializationTask implements AssignmentEndpoint {

  /**
   * Custom ObjectInputStream that implements a strict deserialization whitelist.
   * Only allows specific, known-safe classes to be deserialized, preventing
   * arbitrary code execution via gadget chains.
   */
  private static class SecureObjectInputStream extends ObjectInputStream {
    public SecureObjectInputStream(ByteArrayInputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      String className = desc.getName();

      // Explicitly allow the target class for this lesson
      if (className.equals(VulnerableTaskHolder.class.getName())) {
        return super.resolveClass(desc);
      }

      // Allow primitive types and their array forms (e.g., [B for byte[], [I for int[])
      if (className.startsWith("[") || Class.forName(className).isPrimitive()) {
          return super.resolveClass(desc);
      }

      // Allow common, generally safe Java core classes that might be part of a legitimate
      // serialized object graph. This list should be carefully curated based on application needs.
      if (className.equals("java.lang.String") ||
          className.equals("java.lang.Integer") ||
          className.equals("java.lang.Long") ||
          className.equals("java.lang.Boolean") ||
          className.equals("java.lang.Double") ||
          className.equals("java.lang.Float") ||
          className.equals("java.lang.Byte") ||
          className.equals("java.lang.Short") ||
          className.equals("java.lang.Character") ||
          className.equals("java.util.ArrayList") ||
          className.equals("java.util.LinkedList") ||
          className.equals("java.util.HashMap") ||
          className.equals("java.util.LinkedHashMap") ||
          className.equals("java.util.HashSet") ||
          className.equals("java.util.LinkedHashSet") ||
          className.equals("java.util.Date") ||
          className.equals("java.math.BigDecimal") ||
          className.equals("java.math.BigInteger")
          ) {
          return super.resolveClass(desc);
      }

      // Deny all other classes by throwing an InvalidClassException
      throw new InvalidClassException("Unauthorized deserialization attempt: " + className);
    }
  }

  @PostMapping("/InsecureDeserialization/task")
  @ResponseBody
  public AttackResult completed(@RequestParam String token) throws IOException {
    String b64token;
    long before;
    long after;
    int delay;

    b64token = token.replace('-', '+').replace('_', '/');

    try (ObjectInputStream ois =
        new SecureObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(b64token)))) { // Use SecureObjectInputStream for whitelisting
      before = System.currentTimeMillis();
      Object o = ois.readObject();
      if (!(o instanceof VulnerableTaskHolder)) {
        if (o instanceof String) {
          return failed(this).feedback("insecure-deserialization.stringobject").build();
        }
        return failed(this).feedback("insecure-deserialization.wrongobject").build();
      }
      after = System.currentTimeMillis();
    } catch (InvalidClassException e) {
      // This will now catch InvalidClassException thrown by our SecureObjectInputStream
      // for unauthorized classes, as well as other InvalidClassExceptions.
      return failed(this).feedback("insecure-deserialization.invalidversion").build();
    } catch (IllegalArgumentException e) {
      return failed(this).feedback("insecure-deserialization.expired").build();
    } catch (Exception e) {
      // Catch-all for other deserialization issues.
      return failed(this).feedback("insecure-deserialization.invalidversion").build();
    }

    delay = (int) (after - before);
    if (delay > 7000) {
      return failed(this).build();
    }
    if (delay < 3000) {
      return failed(this).build();
    }
    return success(this).build();
  }
}
