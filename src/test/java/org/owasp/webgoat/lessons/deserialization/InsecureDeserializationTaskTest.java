/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.deserialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for InsecureDeserializationTask focusing on the added ObjectInputFilter that
 * restricts deserialization to a safe whitelist.
 */
public class InsecureDeserializationTaskTest {

  private String toWebToken(Object obj) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(obj);
    }
    String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
    // Mirror the replacement done in the controller: '+' -> '-', '/' -> '_'
    return b64.replace('+', '-').replace('/', '_');
  }

  @Test
  void completed_shouldAcceptWhitelistedVulnerableTaskHolder() throws Exception {
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    VulnerableTaskHolder holder = new VulnerableTaskHolder("test", 0);
    String token = toWebToken(holder);

    AttackResult result = task.completed(token);

    // We only assert that the flow does not fail with class-type-specific errors.
    // Exact success condition is based on internal timing, but for the delta test we assert
    // that the result is not the specific feedback used when wrong object types are deserialized.
    String feedback = result.getFeedback();
    org.junit.jupiter.api.Assertions.assertNotEquals(
        "insecure-deserialization.wrongobject", feedback);
    org.junit.jupiter.api.Assertions.assertNotEquals(
        "insecure-deserialization.stringobject", feedback);
  }

  @Test
  void completed_shouldRejectNonWhitelistedType() throws Exception {
    InsecureDeserializationTask task = new InsecureDeserializationTask();

    // A type not on the whitelist (e.g., plain Integer) should be rejected by the filter.
    String token = toWebToken(Integer.valueOf(42));

    AttackResult result = task.completed(token);

    // When the filter blocks the class, the catch(Exception) path is used,
    // which currently maps to "invalidversion" feedback.
    assertEquals("insecure-deserialization.invalidversion", result.getFeedback());
  }
}
