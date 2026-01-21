package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

// NOTE: Delta tests for the logging change: ensure printStackTrace is no longer used
// and that behavior of getPassword/completed is preserved.
public class SqlInjectionLesson6bTest {

  private LessonDataSource lessonDataSource;
  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;
  private SqlInjectionLesson6b lesson;

  @BeforeEach
  void setUp() throws Exception {
    lessonDataSource = Mockito.mock(LessonDataSource.class);
    connection = Mockito.mock(Connection.class);
    statement = Mockito.mock(Statement.class);
    resultSet = Mockito.mock(ResultSet.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);

    lesson = new SqlInjectionLesson6b(lessonDataSource);
  }

  @Test
  void completed_returnsSuccessWhenUserIdMatchesPassword() throws Exception {
    // Arrange
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("dave");

    // Act
    AttackResult result = lesson.completed("dave");

    // Assert
    assertEquals("success", result.getType().toString().toLowerCase());
  }

  @Test
  void completed_returnsFailureWhenUserIdDoesNotMatchPassword() throws Exception {
    // Arrange
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("dave");

    // Act
    AttackResult result = lesson.completed("not_dave");

    // Assert
    assertEquals("failure", result.getType().toString().toLowerCase());
  }

  @Test
  void getPassword_handlesSqlExceptionWithoutThrowing_printStackTraceRemoved() throws Exception {
    // Arrange
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new SQLException("DB error"));

    // Act
    // The method should swallow the exception and return the default password,
    // and crucially it should use structured logging (log.error) instead of printStackTrace.
    String password = lesson.getPassword();

    // Assert
    assertEquals("dave", password);
    // We cannot easily assert the logging behavior without a logging framework hook,
    // but this test ensures no exception is propagated and behavior is preserved.
  }

  @Test
  void getPassword_handlesGenericExceptionWithoutThrowing() throws Exception {
    // Arrange
    when(lessonDataSource.getConnection()).thenThrow(new RuntimeException("generic"));

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals("dave", password);
  }
}
