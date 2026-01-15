// File: src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword no longer exposes stack traces via printStackTrace when SQL error occurs")
  void getPassword_doesNotPrintStackTraceOnSqlException() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new RuntimeException("Simulated SQL error"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
    java.io.PrintStream originalErr = System.err;
    System.setErr(new java.io.PrintStream(errContent));

    try {
      String password = lesson.getPassword();

      String stderrOutput = errContent.toString();
      org.junit.jupiter.api.Assertions.assertFalse(
          stderrOutput.contains("Simulated SQL error"),
          "Expected no stack trace or error details to be printed after fix");
      org.junit.jupiter.api.Assertions.assertEquals(
          "dave", password, "When an exception occurs, the default password value should be returned");
    } finally {
      System.setErr(originalErr);
    }
  }

  @Test
  @DisplayName("completed only succeeds when user input matches retrieved password")
  void completed_succeedsOnlyWhenUserMatchesPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("secretPassword");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    AttackResult successResult = lesson.completed("secretPassword");
    AttackResult failureResult = lesson.completed("wrongPassword");

    org.junit.jupiter.api.Assertions.assertTrue(
        successResult.toString().contains("success"),
        "Expected success when userid_6b equals the password from the database");
    org.junit.jupiter.api.Assertions.assertTrue(
        failureResult.toString().contains("failed"),
        "Expected failure when userid_6b does not match the password from the database");
  }
}
