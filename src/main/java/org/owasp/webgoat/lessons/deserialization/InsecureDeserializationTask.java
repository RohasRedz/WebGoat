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
   * A custom ObjectInputStream that whitelists allowed classes during deserialization.
   * This prevents deserialization of arbitrary types, mitigating gadget chain attacks.
   */
  private static class SafeObjectInputStream extends ObjectInputStream {
    public SafeObjectInputStream(ByteArrayInputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      String className = desc.getName();

      // Whitelist only the expected class and common safe types
      if (className.equals(VulnerableTaskHolder.class.getName()) ||
          className.equals(String.class.getName()) ||
          className.equals(Integer.class.getName()) ||
          className.equals(Long.class.getName()) ||
          className.equals(Boolean.class.getName()) ||
          className.equals(Byte.class.getName()) ||
          className.equals(Short.class.getName()) ||
          className.equals(Character.class.getName()) ||
          className.equals(Float.class.getName()) ||
          className.equals(Double.class.getName()) ||
          desc.isPrimitive() || // Primitive types (int, boolean, etc.)
          className.startsWith("[Ljava.lang.String;") || // String[]
          className.startsWith("[Ljava.lang.Integer;") || // Integer[]
          className.startsWith("[B") || // byte[]
          className.startsWith("[S") || // short[]
          className.startsWith("[I") || // int[]
          className.startsWith("[J") || // long[]
          className.startsWith("[F") || // float[]
          className.startsWith("[D") || // double[]
          className.startsWith("[Z") || // boolean[]
          className.startsWith("[C")    // char[]
          ) {
        return super.resolveClass(desc);
      }
      // If the class is not in the whitelist, prevent its deserialization
      throw new InvalidClassException("Unauthorized deserialization attempt", className);
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
        new SafeObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(b64token)))) {
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
      // Catch our custom InvalidClassException for unauthorized types
      return failed(this).feedback("insecure-deserialization.unauthorizedtype").build();
    } catch (IllegalArgumentException e) {
      return failed(this).feedback("insecure-deserialization.expired").build();
    } catch (Exception e) {
      // Catch other potential deserialization exceptions (e.g., ClassNotFoundException for whitelisted but missing classes)
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
