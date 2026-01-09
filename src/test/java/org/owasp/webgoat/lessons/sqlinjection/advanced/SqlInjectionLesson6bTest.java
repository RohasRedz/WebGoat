package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging / information exposure fix:
 * - Verify that getPassword() still retrieves the database password when the query succeeds.
 * - Verify that when SQL or generic exceptions occur, getPassword() does NOT rely on stack trace
 *   side effects and simply returns the default password value, without throwing.
 *
 * These tests do not assert logging behavior directly (comments replaced printStackTrace) but
 * focus on preserving functional behavior under error conditions after removing stack trace prints.
 */
public class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword returns DB password when query succeeds")
  void getPassword_returnsDatabasePassword_onSuccess() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("db-secret");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    // Confirm that the method still prefers the DB value over the hardcoded default,
    // ensuring that removal of printStackTrace did not alter the main logic.
    assertEquals("db-secret", password);
  }

  @Test
  @DisplayName("getPassword falls back to default without throwing when SQLException occurs")
  void getPassword_returnsDefault_onSqlExceptionWithoutInformationExposure() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new SQLException("Simulated SQL failure"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    // The method should swallow the exception (no printStackTrace anymore) and return
    // the default "dave" without propagating errors or relying on stack trace logging.
    assertEquals("dave", password);
  }

  @Test
  @DisplayName("getPassword falls back to default without throwing when generic Exception occurs")
  void getPassword_returnsDefault_onGenericException() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);

    // Simulate an exception when obtaining the connection itself.
    when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection failure"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals(
        "dave",
        password,
        "Expected default password when a generic exception occurs, " +
        "confirming behavior is preserved after removing printStackTrace");
  }
}
