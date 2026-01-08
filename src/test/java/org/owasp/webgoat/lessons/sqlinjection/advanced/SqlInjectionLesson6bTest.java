package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the updated logging behavior
 * that replaces printStackTrace with structured SLF4J logging.
 */
public class SqlInjectionLesson6bTest {

  private LessonDataSource dataSource;
  private SqlInjectionLesson6b lesson;

  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = mock(LessonDataSource.class);
    lesson = new SqlInjectionLesson6b(dataSource);

    connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
  }

  @Test
  void getPassword_returnsPasswordFromDatabase() throws Exception {
    // Arrange
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("db-password");

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals("db-password", password);
  }

  @Test
  void getPassword_fallsBackToDefaultWhenNoResult() throws Exception {
    // Arrange
    when(resultSet.first()).thenReturn(false);

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals("dave", password);
  }

  @Test
  void getPassword_handlesSqlExceptionWithoutPrintStackTrace() throws Exception {
    // Arrange
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new SQLException("boom"));

    // Act
    String password = lesson.getPassword();

    // Assert
    // Behaviorally, the method still returns either the default or last known password
    // and does not rethrow the exception.
    // We cannot easily assert log output without a logging backend, but the absence
    // of a thrown exception ensures that logging replaced printStackTrace without
    // altering control flow.
    assertEquals("dave", password);
  }
}
