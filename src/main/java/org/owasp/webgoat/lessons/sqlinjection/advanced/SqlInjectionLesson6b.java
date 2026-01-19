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
import lombok.extern.slf4j.Slf4j; // Added Slf4j import
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j // Added Slf4j annotation
public class SqlInjectionLesson6b implements AssignmentEndpoint {
  private final LessonDataSource dataSource;

  public SqlInjectionLesson6b(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjectionAdvanced/attack6b")
  @ResponseBody
  public AttackResult completed(@RequestParam String userid_6b) throws IOException {
    // The original logic compares userid_6b with the password retrieved from the database.
    // This is a logical flaw in a real application, but for a lesson, it might be intentional.
    // The fix focuses on the logging vulnerability as per the prompt.
    if (userid_6b.equals(getPassword())) {
      return success(this).build();
    } else {
      return failed(this).build();
    }
  }

  protected String getPassword() {
    String password = "dave"; // This hardcoded default is a potential vulnerability (CWE-798)
                              // but the prompt for this batch focuses on "Information Exposure Through Log Files".
                              // For a real fix, this should be removed or managed securely.
    try (Connection connection = dataSource.getConnection()) {
      String query = "SELECT password FROM user_system_data WHERE user_name = 'dave'";
      try {
        Statement statement =
            connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet results = statement.executeQuery(query);

        if (results != null && results.first()) {
          password = results.getString("password");
        }
      } catch (SQLException sqle) {
        // FIX: Replaced printStackTrace with secure logging to prevent information exposure.
        log.error("Database error during password retrieval attempt.", sqle);
        // do nothing (original comment, keeping for minimal diff, but logging is now secure)
      }
    } catch (Exception e) {
      // FIX: Replaced printStackTrace with secure logging to prevent information exposure.
      log.error("Unexpected error during password retrieval process.", e);
      // do nothing (original comment, keeping for minimal diff, but logging is now secure)
    }
    return (password);
  }
}
