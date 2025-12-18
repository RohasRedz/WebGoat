/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement; // Added for PreparedStatement
import java.sql.ResultSet;
import java.sql.SQLException;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j; // Added for logging

@RestController
@Slf4j // Added Slf4j annotation for logging
public class SqlInjectionLesson6b implements AssignmentEndpoint {
  private final LessonDataSource dataSource;

  public SqlInjectionLesson6b(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjectionAdvanced/attack6b")
  @ResponseBody
  public AttackResult completed(@RequestParam String userid_6b) throws IOException {
    String retrievedPassword = getPassword();
    if (retrievedPassword != null && userid_6b.equals(retrievedPassword)) {
      return success(this).build();
    } else {
      return failed(this).build();
    }
  }

  protected String getPassword() {
    String password = null; // FIX: Removed hard-coded default password "dave"
    try (Connection connection = dataSource.getConnection()) {
      // FIX: Using PreparedStatement for database interaction as a best practice,
      // even if the query itself is currently static.
      String query = "SELECT password FROM user_system_data WHERE user_name = ?";
      try (PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setString(1, "dave"); // Parameterize the username literal
        ResultSet results = statement.executeQuery();

        if (results != null && results.first()) {
          password = results.getString("password");
        }
      } catch (SQLException sqle) {
        // FIX: Replaced printStackTrace() with structured logging to prevent Information Exposure (CWE-532, CWE-209).
        // Only log the message, not the full stack trace to avoid leaking internal details.
        log.error("SQL Exception while retrieving password: {}", sqle.getMessage());
        // password remains null, leading to a secure failure.
      }
    } catch (Exception e) {
      // FIX: Replaced printStackTrace() with structured logging to prevent Information Exposure (CWE-532, CWE-209).
      // Only log the message, not the full stack trace to avoid leaking internal details.
      log.error("General Exception while retrieving password: {}", e.getMessage());
      // password remains null, leading to a secure failure.
    }
    return password; // Returns null if password not found or an error occurred.
  }
}
