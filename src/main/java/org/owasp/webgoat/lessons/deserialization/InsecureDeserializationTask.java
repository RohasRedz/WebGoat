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
import java.io.InputStream;
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

  @PostMapping("/InsecureDeserialization/task")
  @ResponseBody
  public AttackResult completed(@RequestParam String token) throws IOException {
    String b64token;
    long before;
    long after;
    int delay;

    b64token = token.replace('-', '+').replace('_', '/');

    try (ObjectInputStream ois =
        new SecureObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(b64token)))) {
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
      // This catch block will now also handle InvalidClassException thrown by SecureObjectInputStream
      return failed(this).feedback("insecure-deserialization.invalidversion").build();
    } catch (IllegalArgumentException e) {
      return failed(this).feedback("insecure-deserialization.expired").build();
    } catch (Exception e) {
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

  /**
   * A custom ObjectInputStream that whitelists allowed classes during deserialization
   * to prevent insecure deserialization vulnerabilities (CWE-502).
   * Only 'VulnerableTaskHolder', primitive types, arrays of objects, and basic java.lang types
   * are permitted to be deserialized.
   */
  private static class SecureObjectInputStream extends ObjectInputStream {
      public SecureObjectInputStream(InputStream in) throws IOException {
          super(in);
      }

      @Override
      protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
          // Whitelist specific classes and types that are expected to be deserialized.
          // Any other class will trigger an InvalidClassException, preventing malicious deserialization.
          if (desc.getName().equals("org.dummy.insecure.framework.VulnerableTaskHolder") ||
              desc.isPrimitive() || // Allow primitive types (e.g., int, boolean)
              desc.getName().startsWith("[L") || // Allow arrays of objects (e.g., [Ljava.lang.String;)
              desc.getName().startsWith("java.lang.")) { // Allow basic Java language types (e.g., String, Integer)
              return super.resolveClass(desc);
          }
          throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
      }
  }
}
