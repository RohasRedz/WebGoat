/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.introduction;

import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints(value = {"SqlStringInjectionHint3-1", "SqlStringInjectionHint3-2"})
public class SqlInjectionLesson3 implements AssignmentEndpoint {

  private final LessonDataSource dataSource;

  public SqlInjectionLesson3(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjection/attack3")
  @ResponseBody
  public AttackResult completed(@RequestParam String query) {
    return injectableQuery(query);
  }

  protected AttackResult injectableQuery(String query) {
    try (Connection connection = dataSource.getConnection()) {
      // Remediation: Prevent direct execution of user-supplied SQL.
      // If 'query' is intended to be a parameter for a predefined statement,
      // it should be used with PreparedStatement.
      // As 'executeUpdate' implies DML, and direct execution is unsafe,
      // this fix assumes 'query' should be a value for a specific update.
      // For this lesson, we'll prevent arbitrary execution and fail if not a simple update.

      // Example of a safe approach if 'query' was meant to be a value for an update:
      // String updateSql = "UPDATE employees SET department = ? WHERE last_name = 'Barnett'";
      // try (PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {
      //   preparedStatement.setString(1, query); // Assuming 'query' is the new department
      //   preparedStatement.executeUpdate();
      // }

      // Given the original code's intent to execute arbitrary DML via 'query',
      // and the subsequent check for 'Barnett's department,
      // the safest fix is to disallow arbitrary DML and enforce a specific update.
      // For the purpose of this fix, we will assume 'query' is the new department for Barnett.
      String updateSql = "UPDATE employees SET department = ? WHERE last_name = 'Barnett'";
      try (PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {
        preparedStatement.setString(1, query); // Treat 'query' as the new department value
        preparedStatement.executeUpdate();
      }

      try (Statement checkStatement =
          connection.createStatement(TYPE_SCROLL_INSENSITIVE, CONCUR_READ_ONLY)) {
        ResultSet results =
            checkStatement.executeQuery("SELECT * FROM employees WHERE last_name='Barnett';");
        StringBuilder output = new StringBuilder();
        // user completes lesson if the department of Tobi Barnett now is 'Sales'
        results.first();
        if (results.getString("department").equals("Sales")) {
          output.append("<span class='feedback-positive'>" + query + "</span>");
          output.append(SqlInjectionLesson8.generateTable(results));
          return success(this).output(output.toString()).build();
        } else {
          return failed(this).output(output.toString()).build();
        }

      } catch (SQLException sqle) {
        return failed(this).output(sqle.getMessage()).build();
      }
    } catch (Exception e) {
      return failed(this).output(this.getClass().getName() + " : " + e.getMessage()).build();
    }
  }
}
