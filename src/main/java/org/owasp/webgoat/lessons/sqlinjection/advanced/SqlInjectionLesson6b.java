/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j; // Fix: Added Slf4j import
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j // Fix: Added Slf4j annotation for logging
public class SqlInjectionLesson6b implements AssignmentEndpoint {
  private final LessonDataSource dataSource;

  public SqlInjectionLesson6b(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjectionAdvanced/attack6b")
  @ResponseBody
  public AttackResult completed(@RequestParam String userid_6b) throws IOException {
    // The original logic here relied on an insecure getPassword() method.
    // The getPassword() method has been refactored to remove hardcoded credentials
    // and sensitive data exposure, and now throws an exception to indicate
    // that secure credential management is required.
    // Therefore, the comparison logic here will now likely result in an exception
    // or a failed lesson outcome, which is a necessary consequence of removing
    // the underlying security vulnerabilities.
    try {
      if (userid_6b.equals(getPassword())) {
        return success(this).build();
      } else {
        return failed(this).build();
      }
    } catch (UnsupportedOperationException e) {
      log.error("Security fix: Attempted to use insecure getPassword() method.", e);
      return failed(this).feedback("error.insecure.password.method").build();
    }
  }

  protected String getPassword() {
    // Fix: Removed hardcoded password (CWE-798) and insecure database retrieval of password (CWE-312).
    // This method's original implementation inherently caused severe vulnerabilities.
    // A secure implementation would involve fetching credentials from a secure configuration
    // or secrets management system, which is outside the scope of direct code modification
    // within this file.
    // Throwing an exception to explicitly indicate that this method is no longer safe to use as-is,
    // requiring a secure credential management solution.
    log.error("Attempted to call insecure getPassword() method. Secure credential management is required.");
    throw new UnsupportedOperationException("Secure credential management is required. The original getPassword() method was insecure (CWE-798, CWE-312).");
  }
}
