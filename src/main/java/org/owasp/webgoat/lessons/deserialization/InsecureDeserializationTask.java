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
import java.io.ObjectStreamClass; // Added import for ObjectStreamClass
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

  // Custom ObjectInputStream to restrict deserialization to allowed classes
  private static class ValidatingObjectInputStream extends ObjectInputStream {
    public ValidatingObjectInputStream(ByteArrayInputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      // Allow only specific classes for deserialization
      if (desc.getName().equals(VulnerableTaskHolder.class.getName()) ||
          desc.getName().equals(String.class.getName()) ||
          desc.getName().equals(Integer.class.getName()) || // Example: if primitive wrappers are expected
          desc.getName().equals(Long.class.getName()) ||
          desc.getName().equals(Boolean.class.getName()) ||
          desc.getName().startsWith("[Ljava.lang.String;") // Allow String arrays
          ) {
        return super.resolveClass(desc);
      }
      // For any other class, throw an exception to prevent deserialization
      throw new InvalidClassException("Unauthorized deserialization attempt for class " + desc.getName());
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

    // FIX: Using a custom ValidatingObjectInputStream to prevent deserialization of arbitrary classes
    try (ObjectInputStream ois =
        new ValidatingObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(b64token)))) { // Changed to use ValidatingObjectInputStream
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
      // Catching InvalidClassException specifically for unauthorized deserialization attempts
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
}
