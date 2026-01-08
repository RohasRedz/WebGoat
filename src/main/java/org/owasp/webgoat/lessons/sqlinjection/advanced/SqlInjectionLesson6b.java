/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement; // Added import for PreparedStatement
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j; // Added import for Slf4j
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
    if (userid_6b.equals(getPassword())) {
      return success(this).build();
    } else {
      return failed(this).build();
    }
  }

  protected String getPassword() {
    String password = null; // FIX: Removed hardcoded fallback password "dave"
    try (Connection connection = dataSource.getConnection()) {
      // FIX: Converted to PreparedStatement to prevent unsafe SQL pattern and ensure parameterization
      String query = "SELECT password FROM user_system_data WHERE user_name = ?";
      try (PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setString(1, "dave"); // Parameterizing the fixed username
        ResultSet results = statement.executeQuery();

        if (results != null && results.first()) {
          password = results.getString("password");
        }
      } catch (SQLException sqle) {
        log.error("SQL error while fetching password", sqle); // FIX: Replaced printStackTrace with secure logging
      }
    } catch (Exception e) {
      log.error("Error while fetching password", e); // FIX: Replaced printStackTrace with secure logging
    }
    return (password);
  }
}
