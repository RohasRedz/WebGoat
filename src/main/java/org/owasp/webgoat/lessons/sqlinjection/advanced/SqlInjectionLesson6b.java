/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class SqlInjectionLesson6b implements AssignmentEndpoint {
  private final LessonDataSource dataSource;

  public SqlInjectionLesson6b(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjectionAdvanced/attack6b")
  @ResponseBody
  public AttackResult completed(@RequestParam String userid_6b) throws IOException {
    if (userid_6b.equals(getPassword())) {
      return success(this).build();
    } else {
      return failed(this).build();
    }
  }

  protected String getPassword() {
    String password = null; // Initialize to null, no hardcoded default
    try (Connection connection = dataSource.getConnection()) {
      // Use PreparedStatement to prevent SQL Injection and parameterize the username
      String query = "SELECT password FROM user_system_data WHERE user_name = ?";
      try (PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setString(1, "dave"); // 'dave' is hardcoded in the original query, so parameterize it.
        ResultSet results = statement.executeQuery();

        if (results != null && results.first()) {
          password = results.getString("password");
        }
      } catch (SQLException sqle) {
        log.error("Database error while retrieving password: {}", sqle.getMessage()); // Log securely
      }
    } catch (Exception e) {
      log.error("An unexpected error occurred while retrieving password: {}", e.getMessage()); // Log securely
    }
    return password; // Return null if not found or error, instead of hardcoded 'dave'
  }
}
