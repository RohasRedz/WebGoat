package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the change from printStackTrace()
 * to structured logging via SLF4J (using Lombok's @Slf4j in the updated class).
 *
 * Since Lombok generates the 'log' field, we cannot access it directly from tests,
 * but we can verify functional behavior of getPassword() under normal and exceptional
 * conditions to ensure that stack traces are not thrown or leaked via exceptions.
 */
public class SqlInjectionLesson6bTest {

  /**
   * A lightweight subclass that exposes getPassword() for testing and lets us inject
   * a mocked LessonDataSource.
   */
  static class TestableSqlInjectionLesson6b extends SqlInjectionLesson6b {
    public TestableSqlInjectionLesson6b(LessonDataSource dataSource) {
      super(dataSource);
    }

    @Override
    public String getPassword() {
      return super.getPassword();
    }
  }

  @Test
  @DisplayName("getPassword uses database value when query succeeds and does not alter behavior")
  void getPassword_usesDatabaseValue_whenQuerySucceeds() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(lessonDataSource.getConnection()).thenReturn(connection);
    Mockito.when(
            connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    Mockito.when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
    Mockito.when(resultSet.first()).thenReturn(true);
    Mockito.when(resultSet.getString("password")).thenReturn("db-password");

    TestableSqlInjectionLesson6b lesson = new TestableSqlInjectionLesson6b(lessonDataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals("db-password", password, "Should return database password when query succeeds");
  }

  @Test
  @DisplayName("getPassword returns default when SQL exception occurs without propagating stack trace")
  void getPassword_handlesSqlException_andReturnsDefault() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);

    Mockito.when(lessonDataSource.getConnection()).thenReturn(connection);
    Mockito.when(
            connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new SQLException("Simulated SQL failure"));

    TestableSqlInjectionLesson6b lesson = new TestableSqlInjectionLesson6b(lessonDataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    // When an exception occurs, the method should return the default "dave"
    // and must not throw the exception (stack trace printing is now replaced by logging).
    assertEquals("dave", password, "Should fall back to default password on SQL exception");
  }

  @Test
  @DisplayName("getPassword returns default when generic exception occurs without propagating stack trace")
  void getPassword_handlesGenericException_andReturnsDefault() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);

    // Simulate exception when acquiring connection itself
    Mockito.when(lessonDataSource.getConnection())
        .thenThrow(new RuntimeException("Simulated connection failure"));

    TestableSqlInjectionLesson6b lesson = new TestableSqlInjectionLesson6b(lessonDataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals("dave", password, "Should fall back to default password on generic exception");
  }
}
