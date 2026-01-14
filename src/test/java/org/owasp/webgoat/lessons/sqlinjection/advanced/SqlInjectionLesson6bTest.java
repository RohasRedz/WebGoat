package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging change that replaced
 * printStackTrace() with structured logging via SLF4J.
 *
 * Since we cannot easily intercept Lombok-generated logger internals here, this test
 * focuses on ensuring that getPassword() still returns the default value when a
 * SQLException occurs (i.e., stack trace is no longer leaked but behavior is preserved).
 */
@Slf4j
public class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword returns default value when SQLException occurs (no stack trace dependency)")
  void getPassword_returnsDefaultOnSQLException() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);

    when(dataSource.getConnection()).thenReturn(connection);
    // Force a SQLException when creating a Statement, simulating a DB issue
    when(connection.createStatement(
            Mockito.anyInt(),
            Mockito.anyInt()))
        .thenThrow(new SQLException("Simulated DB failure"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    // The default value "dave" should be returned when an exception is thrown
    assertEquals("dave", password);
  }

  @Test
  @DisplayName("getPassword reads value from DB when no exception occurs")
  void getPassword_readsValueFromDatabase() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            Mockito.anyInt(),
            Mockito.anyInt()))
        .thenReturn(statement);
    when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("dbPassword");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals("dbPassword", password);
  }
}
