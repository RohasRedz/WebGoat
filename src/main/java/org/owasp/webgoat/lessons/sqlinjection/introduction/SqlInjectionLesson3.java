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
import java.sql.PreparedStatement; // Added for PreparedStatement
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
      // Remediation: Do not directly execute user-supplied arbitrary SQL queries.
      // Instead, parse the query to extract intended parameters and use PreparedStatement.
      // For this lesson, assuming the 'query' is intended to update the department of 'Tobi Barnett'.
      // If the query is truly arbitrary, a more robust solution would be to reject it or use a SQL parser/whitelist.
      if (!query.toLowerCase().startsWith("update employees set department=") || !query.toLowerCase().contains("where last_name='barnett'")) {
          return failed(this).output("Only specific UPDATE queries for 'Tobi Barnett' are allowed.").build();
      }
      
      // Extract the department value from the query string
      String department = null;
      try {
          int startIndex = query.indexOf("department='") + "department='".length();
          int endIndex = query.indexOf("'", startIndex);
          if (startIndex > -1 && endIndex > startIndex) {
              department = query.substring(startIndex, endIndex);
          }
      } catch (Exception e) {
          return failed(this).output("Invalid query format.").build();
      }

      if (department == null) {
          return failed(this).output("Department not found in query.").build();
      }

      try (PreparedStatement updateStatement = connection.prepareStatement(
          "UPDATE employees SET department = ? WHERE last_name = 'Barnett'")) {
        updateStatement.setString(1, department);
        updateStatement.executeUpdate();

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
        }
      } catch (SQLException sqle) {
        return failed(this).output(sqle.getMessage()).build();
      }
    } catch (Exception e) {
      return failed(this).output(this.getClass().getName() + " : " + e.getMessage()).build();
    }
  }
}
