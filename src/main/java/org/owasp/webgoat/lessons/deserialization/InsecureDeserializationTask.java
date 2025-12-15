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
import java.io.ObjectInputFilter; // Added for JEP 290 deserialization filter
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
        new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(b64token)))) {
      // Apply JEP 290 deserialization filter to restrict allowed classes.
      // This filter whitelists only the expected VulnerableTaskHolder and common primitive wrappers/String,
      // effectively preventing deserialization of arbitrary malicious classes.
      // It also adds limits (maxdepth, maxreferences, maxbytes) to mitigate Denial of Service (DoS) attacks
      // that could be caused by overly complex or large object graphs.
      ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
          "org.dummy.insecure.framework.VulnerableTaskHolder;" + // Whitelist the expected class
          "java.lang.String;java.lang.Integer;java.lang.Long;java.lang.Boolean;" + // Whitelist common primitive wrappers
          "java.util.ArrayList;java.util.LinkedList;java.util.HashMap;java.util.HashSet;" + // Whitelist common collections if they might be part of the graph
          "maxdepth=10;maxreferences=100;maxbytes=100000;" + // Add limits to prevent DoS attacks
          "!*" // Reject all other classes not explicitly whitelisted
      );
      ois.setObjectInputFilter(filter); // Apply the filter to this specific ObjectInputStream instance

      before = System.currentTimeMillis();
      Object o = ois.readObject();
      after = System.currentTimeMillis(); // Measure time immediately after deserialization

      if (!(o instanceof VulnerableTaskHolder)) {
        if (o instanceof String) {
          return failed(this).feedback("insecure-deserialization.stringobject").build();
        }
        return failed(this).feedback("insecure-deserialization.wrongobject").build();
      }
      // The original code had 'after = System.currentTimeMillis();' here,
      // but it should be right after readObject() to measure deserialization time.
      // The current placement is more accurate for measuring deserialization delay.

    } catch (InvalidClassException e) {
      return failed(this).feedback("insecure-deserialization.invalidversion").build();
    } catch (IllegalArgumentException e) {
      return failed(this).feedback("insecure-deserialization.expired").build();
    } catch (Exception e) {
      // This catch block will now also handle ObjectInputFilter.FilterException
      // if an unauthorized class is attempted to be deserialized, providing a generic error.
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
