/*
 * SPDX-FileCopyrightText: Copyright © 2017 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SqlInjectionLesson6b implements AssignmentEndpoint {

  private final LessonDataSource dataSource;

  @PostMapping("SqlInjectionAdvanced/attack6b")
  @ResponseBody
  public AttackResult completed(@RequestParam String name) {
    try (Connection connection = dataSource.getConnection()) {
      PreparedStatement statement =
          connection.prepareStatement("SELECT * FROM user_data WHERE last_name = ?");
      statement.setString(1, name);
      ResultSet results = statement.executeQuery();

      if (results.next()) {
        return success(this).feedback("sql-injection.6b.success").build();
      } else {
        return failed(this).feedback("sql-injection.6b.no.results").build();
      }
    } catch (SQLException e) {
      log.error("An SQL error occurred during database operation.", e);
      return failed(this).build();
    }
  }
}
