/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement; // Added for secure password check
import java.sql.ResultSet;
import java.sql.SQLException;
// import java.sql.Statement; // Removed as PreparedStatement is used
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j // Added for logging
public class SqlInjectionLesson6b implements AssignmentEndpoint {
  private final LessonDataSource dataSource;

  public SqlInjectionLesson6b(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjectionAdvanced/attack6b")
  @ResponseBody
  public AttackResult completed(@RequestParam String userid_6b) throws IOException {
    // FIX: Refactored to use a secure password checking method that does not expose the raw password
    if (checkPasswordForDave(userid_6b)) {
      return success(this).build();
    } else {
      return failed(this).build();
    }
  }

  /**
   * FIX: Refactored method to securely check a provided password against the stored password for 'dave'.
   * This prevents exposing the raw password from the database.
   * Uses PreparedStatement to prevent SQL injection in the password check itself.
   *
   * @param providedPassword The password provided by the user.
   * @return true if the provided password matches the stored password for 'dave', false otherwise.
   */
  protected boolean checkPasswordForDave(String providedPassword) {
    try (Connection connection = dataSource.getConnection()) {
      // Use PreparedStatement to securely check the password
      String query = "SELECT password FROM user_system_data WHERE user_name = 'dave' AND password = ?";
      try (PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setString(1, providedPassword);
        try (ResultSet results = statement.executeQuery()) {
          return results.next(); // If a row is returned, the password matches
        }
      }
    } catch (SQLException e) {
      // FIX: Replaced printStackTrace with secure logging to avoid information exposure
      log.error("Database error during password check for 'dave': {}", e.getMessage());
      return false;
    }
  }

  // FIX: Removed the original getPassword() method as it exposed the raw password.
  // The functionality is replaced by checkPasswordForDave(String providedPassword).
}
