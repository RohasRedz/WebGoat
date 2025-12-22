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
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SqlInjectionLesson6b implements AssignmentEndpoint {
  private final LessonDataSource dataSource;

  public SqlInjectionLesson6b(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjectionAdvanced/attack6b")
  @ResponseBody
  public AttackResult completed(@RequestParam String userid_6b) throws IOException {
    if (checkPassword(userid_6b)) {
      return success(this).build();
    } else {
      return failed(this).build();
    }
  }

  protected boolean checkPassword(String providedPassword) {
    String storedPassword = null;
    try (Connection connection = dataSource.getConnection()) {
      String query = "SELECT password FROM user_system_data WHERE user_name = 'dave'";
      try (
          Statement statement =
              connection.createStatement(
                  ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
          ResultSet results = statement.executeQuery(query)) {

        if (results != null && results.first()) {
          storedPassword = results.getString("password");
        }
      } catch (SQLException sqle) {
        // Log the exception securely, avoid exposing sensitive information or stack traces directly to output.
        // Example: log.error("Database error during password check: {}", sqle.getMessage());
      }
    } catch (Exception e) {
      // Log the exception securely, avoid exposing sensitive information or stack traces directly to output.
      // Example: log.error("General error during password check: {}", e.getMessage());
    }
    return storedPassword != null && storedPassword.equals(providedPassword);
  }
}
