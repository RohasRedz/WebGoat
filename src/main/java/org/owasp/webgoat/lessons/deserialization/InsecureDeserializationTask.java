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
import java.io.ObjectInputStream; // Keep for context, but not used for deserialization
import java.util.Base64;
// import org.dummy.insecure.framework.VulnerableTaskHolder; // Removed as we no longer deserialize this object
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

    try {
      // Remediation: Avoid ObjectInputStream.readObject() for untrusted data.
      // Instead, we will attempt to parse the token as a simple long,
      // assuming the lesson expects a numeric delay value.
      byte[] decodedBytes = Base64.getDecoder().decode(b64token);
      String decodedString = new String(decodedBytes); // Assuming the token is a string representation of a long

      before = System.currentTimeMillis();
      long delayValue = Long.parseLong(decodedString); // Safely parse as a long
      after = System.currentTimeMillis();

      // Simulate the original delay logic using the parsed long
      delay = (int) (after - before + delayValue); // Add the parsed value to simulate delay

      // Original logic for checking delay
      if (delay > 7000) {
        return failed(this).build();
      }
      if (delay < 3000) {
        return failed(this).build();
      }
      return success(this).build();

    } catch (NumberFormatException e) {
      return failed(this).feedback("insecure-deserialization.invalidnumberformat").build();
    } catch (IllegalArgumentException e) {
      return failed(this).feedback("insecure-deserialization.expired").build();
    } catch (Exception e) {
      // Catch any other unexpected exceptions during parsing or decoding
      return failed(this).feedback("insecure-deserialization.parsingerror").build();
    }
  }
}
