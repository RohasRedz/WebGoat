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
import java.io.InputStream; // Added for SecureObjectInputStream constructor
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass; // Added for resolveClass override
import java.util.Base64;
import java.util.Set; // Added for allowlist
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

  // SVCF-319 Remediation: Define an explicit allowlist of classes that are permitted to be deserialized.
  // This prevents deserialization of arbitrary malicious classes.
  private static final Set<String> DESERIALIZATION_ALLOWED_CLASSES = Set.of(
      VulnerableTaskHolder.class.getName(),
      String.class.getName() // Required for the existing 'if (o instanceof String)' check
  );

  // SVCF-319 Remediation: Custom ObjectInputStream to enforce the class allowlist.
  // By overriding resolveClass, we can control which classes are allowed to be loaded during deserialization.
  private static class SecureObjectInputStream extends ObjectInputStream {
      public SecureObjectInputStream(InputStream in) throws IOException {
          super(in);
      }

      @Override
      protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
          // SVCF-319 Remediation: If the class is not in our allowlist, throw an InvalidClassException
          // to prevent its deserialization.
          if (!DESERIALIZATION_ALLOWED_CLASSES.contains(desc.getName())) {
              throw new InvalidClassException("Unauthorized deserialization attempt: " + desc.getName());
          }
          // If the class is allowed, proceed with default resolution.
          return super.resolveClass(desc);
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

    // SVCF-319 Remediation: Perform input validation on the decoded byte array.
    // This helps prevent resource exhaustion attacks (e.g., very large serialized objects)
    // and ensures the input is not empty before attempting deserialization.
    byte[] decodedBytes = Base64.getDecoder().decode(b64token);
    final int MAX_DESERIALIZED_SIZE = 4096; // Example: 4KB limit for serialized object size
    if (decodedBytes.length == 0 || decodedBytes.length > MAX_DESERIALIZED_SIZE) {
        return failed(this).feedback("insecure-deserialization.invalidinputsize").build();
    }

    try (ObjectInputStream ois =
        new SecureObjectInputStream(new ByteArrayInputStream(decodedBytes))) { // SVCF-319: Use custom SecureObjectInputStream
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
      // SVCF-319 Remediation: Catch InvalidClassException specifically for unauthorized classes
      // or serialVersionUID mismatches.
      return failed(this).feedback("insecure-deserialization.unauthorizedclass").build();
    } catch (IllegalArgumentException e) {
      return failed(this).feedback("insecure-deserialization.expired").build();
    } catch (Exception e) {
      // SVCF-319 Remediation: Catch other general deserialization exceptions and provide a generic error.
      // Avoid leaking internal exception details.
      return failed(this).feedback("insecure-deserialization.invalidformat").build();
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
