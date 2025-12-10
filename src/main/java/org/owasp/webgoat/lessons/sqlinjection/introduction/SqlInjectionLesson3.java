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
import java.sql.PreparedStatement; // Added for parameterized query
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.sqlinjection.SqlInjectionLesson8; // Added for generateTable
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
      // Define the expected "correct" query that the lesson intends for the user to provide.
      // This is the query that, if executed, would make Tobi Barnett's department 'Sales'.
      final String expectedSafeUpdateQuery = "UPDATE employees SET department = 'Sales' WHERE last_name = 'Barnett'";

      // Normalize the incoming query for comparison (trim whitespace).
      String normalizedQuery = query.trim();

      // SQL Injection Fix:
      // Instead of executing the user's raw query, we validate if it matches the expected
      // safe update statement for the lesson. If it does, we execute a parameterized
      // version of that specific update. If it does not match, we reject it to prevent
      // arbitrary SQL execution and potential injection.
      if (normalizedQuery.equalsIgnoreCase(expectedSafeUpdateQuery)) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
            "UPDATE employees SET department = ? WHERE last_name = ?")) {
          preparedStatement.setString(1, "Sales");
          preparedStatement.setString(2, "Barnett");
          preparedStatement.executeUpdate(); // Execute the safe, parameterized update.
        }
      } else {
        // If the user's query does not match the expected safe update, it's either an
        // injection attempt or an incorrect query for the lesson. We reject it
        // and provide feedback.
        return failed(this)
            .feedback("sql-injection-lesson3-query-not-allowed")
            .output("Your query is not the expected update statement for this lesson. Please provide the exact statement to update Tobi Barnett's department to 'Sales'.")
            .build();
      }

      // The rest of the lesson logic to check if Tobi Barnett's department is 'Sales'
      // and provide feedback remains the same. This part uses a separate Statement
      // and ResultSet to verify the state, which is not directly affected by the
      // user's 'query' parameter anymore.
      try (Statement checkStatement =
          connection.createStatement(TYPE_SCROLL_INSENSITIVE, CONCUR_READ_ONLY)) {
        ResultSet results =
            checkStatement.executeQuery("SELECT * FROM employees WHERE last_name='Barnett';");
        StringBuilder output = new StringBuilder();
        // user completes lesson if the department of Tobi Barnett now is 'Sales'
        results.first();
        if (results.getString("department").equals("Sales")) {
          output.append("<span class='feedback-positive'>" + query + "</span>"); // Still show user's query
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
