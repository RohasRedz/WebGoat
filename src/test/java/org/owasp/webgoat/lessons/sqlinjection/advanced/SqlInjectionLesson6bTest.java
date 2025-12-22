package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on:
 * - getPassword() still returning the expected password.
 * - Exceptions are logged via SLF4J logger, not printStackTrace.
 */
class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword should return password from database when query succeeds")
  void getPasswordReturnsPasswordFromDatabase() throws Exception {
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
    when(resultSet.getString("password")).thenReturn("securePasswordFromDb");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals("securePasswordFromDb", password);
  }

  @Test
  @DisplayName("getPassword should log SQLExceptions via logger instead of using printStackTrace")
  void getPasswordLogsSqlExceptionsWithLogger() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenThrow(new SQLException("DB error"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Spy on the logger used by the @Slf4j annotation
    Logger originalLogger = LoggerFactory.getLogger(SqlInjectionLesson6b.class);
    Logger spyLogger = spy(originalLogger);

    // Use reflection to replace the static logger field if Lombok generated one.
    // If this fails in a particular environment, this test can be adapted accordingly.
    try {
      var field = SqlInjectionLesson6b.class.getDeclaredField("log");
      field.setAccessible(true);
      field.set(null, spyLogger);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      // TODO: Adjust if logger field name or visibility differs in this project setup.
      throw new IllegalStateException("Unable to inject spy logger for test", e);
    }

    // Act
    String password = lesson.getPassword();

    // Assert: default password should be returned when exception occurs
    assertEquals("dave", password, "On failure, getPassword should fall back to default value");

    // Verify that logger.error was called with our message and exception
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
    verify(spyLogger, atLeastOnce()).error(messageCaptor.capture(), throwableCaptor.capture());

    boolean containsExpectedMessage =
        messageCaptor.getAllValues().stream()
            .anyMatch(msg -> msg.contains("SQL Exception occurred while fetching password"));
    assertEquals(
        true,
        containsExpectedMessage,
        "Expected an error log message about SQL Exception when fetching password");
  }
}
