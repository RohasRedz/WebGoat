/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.deserialization;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.IOException;
import java.util.Base64;
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
    int simulatedDelay = 0; // Initialize simulated delay

    b64token = token.replace('-', '+').replace('_', '/');

    try {
      // FIX: Replaced insecure ObjectInputStream.readObject() with safe parsing of user-controlled data.
      // The original lesson likely intended for a delay value to be passed via the token.
      // We now assume the base64 decoded token is a string representation of an integer delay.
      byte[] decodedBytes = Base64.getDecoder().decode(b64token);
      String decodedString = new String(decodedBytes);

      // Attempt to parse the decoded string as an integer, representing the intended delay.
      simulatedDelay = Integer.parseInt(decodedString);

      // Simulate the "task" execution time based on the parsed delay.
      // This preserves the lesson's timing aspect without relying on insecure Java deserialization.
      before = System.currentTimeMillis();
      Thread.sleep(simulatedDelay); // Simulate the work/delay
      after = System.currentTimeMillis();

    } catch (NumberFormatException e) {
      // If the decoded string is not a valid integer, it's not the expected format.
      // This replaces the "wrong object" or "string object" feedback from the original vulnerable code.
      return failed(this).feedback("insecure-deserialization.invalidtokenformat").build();
    } catch (InterruptedException e) {
      // Handle cases where the thread sleep is interrupted
      Thread.currentThread().interrupt(); // Restore the interrupted status
      return failed(this).feedback("insecure-deserialization.interrupted").build();
    } catch (IllegalArgumentException e) {
      // Catch Base64 decoding errors (e.g., malformed base64 string)
      return failed(this).feedback("insecure-deserialization.malformedbase64").build();
    } catch (Exception e) {
      // Catch any other unexpected exceptions during processing
      return failed(this).feedback("insecure-deserialization.processingerror").build();
    }

    delay = (int) (after - before);
    // The original lesson had checks for delay > 7000 and delay < 3000.
    // This logic is preserved, now based on the safely simulated delay.
    if (delay > 7000) {
      return failed(this).build();
    }
    if (delay < 3000) {
      return failed(this).build();
    }
    return success(this).build();
  }
}
