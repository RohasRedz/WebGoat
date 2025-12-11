/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.deserialization;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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
        new SecureObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(b64token)))) { // Using custom SecureObjectInputStream
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
      // This catch block now also handles unauthorized classes rejected by SecureObjectInputStream
      return failed(this).feedback("insecure-deserialization.invalidversion").build();
    } catch (IllegalArgumentException e) {
      return failed(this).feedback("insecure-deserialization.expired").build();
    } catch (Exception e) {
      // General catch-all for other deserialization issues
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
   * Custom ObjectInputStream that whitelists allowed classes during deserialization.
   * This prevents deserialization of arbitrary types, mitigating RCE vulnerabilities
   * associated with insecure deserialization.
   */
  private static class SecureObjectInputStream extends ObjectInputStream {

    // Define a whitelist of classes that are allowed to be deserialized.
    // Based on the application logic, only VulnerableTaskHolder and String are expected.
    private static final List<String> ALLOWED_CLASSES = Arrays.asList(
        "org.dummy.insecure.framework.VulnerableTaskHolder",
        "java.lang.String"
    );

    public SecureObjectInputStream(InputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      // If the class being deserialized is not in our whitelist, throw an exception.
      if (!ALLOWED_CLASSES.contains(desc.getName())) {
        throw new InvalidClassException(
            "Unauthorized deserialization attempt: " + desc.getName() + " is not whitelisted."
        );
      }
      // Otherwise, proceed with default deserialization for whitelisted classes.
      return super.resolveClass(desc);
    }
  }
}
