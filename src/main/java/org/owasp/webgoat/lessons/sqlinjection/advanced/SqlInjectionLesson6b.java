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
    if (verifyPassword(userid_6b)) {
      return success(this).build();
    } else {
      return failed(this).build();
    }
  }

  protected boolean verifyPassword(String inputPassword) {
    boolean passwordMatches = false;
    String query = "SELECT password FROM user_system_data WHERE user_name = 'dave'";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(query)) {
      ResultSet results = statement.executeQuery();

      if (results.next()) {
        String storedPassword = results.getString("password");
        passwordMatches = inputPassword.equals(storedPassword);
      }
    } catch (SQLException sqle) {
      // secure logging recommended, no stack trace
    } catch (Exception e) {
      // Catch other potential exceptions and handle securely, avoiding printStackTrace().
    }
    return passwordMatches;
  }
}
