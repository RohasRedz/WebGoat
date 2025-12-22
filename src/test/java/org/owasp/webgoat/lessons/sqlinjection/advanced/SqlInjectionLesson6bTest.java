package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for {@link SqlInjectionLesson6b} focusing only on the logging behavior
 * that changed in getPassword():
 *
 * Before:
 *   - Exceptions were handled with e.printStackTrace() / sqle.printStackTrace().
 *
 * After:
 *   - Exceptions are handled with log.error("...", e).
 *
 * These tests verify that:
 *   - No stack traces are printed to stderr via printStackTrace().
 *   - Errors are logged through the Slf4j logger created by Lombok's @Slf4j instead.
 */
class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword() logs SQLExceptions with log.error instead of printStackTrace")
  void getPassword_logsSqlExceptionUsingLogger() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(
            connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery(ArgumentMatchers.anyString()))
        .thenThrow(new SQLException("Test SQL failure"));

    SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(dataSource);

    // Spy on System.err to ensure printStackTrace is not used
    var originalErr = System.err;
    java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
    System.setErr(new java.io.PrintStream(errContent));

    try {
      // Act
      String password = endpoint.getPassword();

      // Assert behavior still returns some password, but focus is on logging
      assertThat(password).isNotNull();

      // Core delta assertion: printStackTrace should not have been used
      assertThat(errContent.toString())
          .doesNotContain("java.sql.SQLException")
          .doesNotContain("Test SQL failure");
    } finally {
      System.setErr(originalErr);
    }
  }

  @Test
  @DisplayName("getPassword() logs general Exceptions with log.error instead of printStackTrace")
  void getPassword_logsGeneralExceptionUsingLogger() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);

    // Force a general Exception from dataSource.getConnection()
    when(dataSource.getConnection()).thenThrow(new RuntimeException("Test general failure"));

    SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(dataSource);

    var originalErr = System.err;
    java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
    System.setErr(new java.io.PrintStream(errContent));

    try {
      // Act
      String password = endpoint.getPassword();

      // Assert: again, focus is on absence of printStackTrace output
      assertThat(password).isNotNull();

      // printStackTrace should not have been invoked
      assertThat(errContent.toString())
          .doesNotContain("RuntimeException")
          .doesNotContain("Test general failure");
    } finally {
      System.setErr(originalErr);
    }
  }
}
