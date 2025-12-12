/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.deserialization;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.IOException;
// Removed: import java.io.ByteArrayInputStream;
// Removed: import java.io.InvalidClassException;
// Removed: import java.io.ObjectInputStream;
import java.util.Base64;
// Removed: import org.dummy.insecure.framework.VulnerableTaskHolder;
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
    int delay = 0; // Initialize delay, will be parsed directly from token

    b64token = token.replace('-', '+').replace('_', '/');

    // Remediation: Replaced insecure ObjectInputStream deserialization with secure parsing
    // The lesson's original intent was to demonstrate time-based deserialization.
    // To prevent the vulnerability while preserving lesson outcome logic,
    // the token is now expected to directly contain the 'delay' value as a string.
    try {
        String decodedString = new String(Base64.getDecoder().decode(b64token));
        delay = Integer.parseInt(decodedString); // Securely parse the delay value
    } catch (IllegalArgumentException e) {
        // Handle invalid Base64 encoding
        return failed(this).feedback("insecure-deserialization.invalidtokenformat").build();
    } catch (NumberFormatException e) {
        // Handle cases where the decoded string is not a valid number for delay
        return failed(this).feedback("insecure-deserialization.invaliddelayvalue").build();
    } catch (Exception e) {
        // Catch any other unexpected exceptions during decoding/parsing
        return failed(this).feedback("insecure-deserialization.processingerror").build();
    }

    // The lesson's success/failure criteria based on delay remain,
    // but the delay is now securely provided by the token, not insecure deserialization.
    if (delay > 7000) {
      return failed(this).build();
    }
    if (delay < 3000) {
      return failed(this).build();
    }
    return success(this).build();
  }
}
