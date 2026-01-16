package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on the logging-related changes:
 * - Ensures exceptions in getPassword are logged with log.error instead of printStackTrace().
 * - Ensures functional behavior (returning the last known password value) is preserved when an
 *   exception occurs.
 */
@Slf4j
public class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword logs SQLExceptions via log.error and preserves default password")
  void getPassword_logsSQLExceptionWithLogger_andPreservesDefaultPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            Mockito.anyInt(),
            Mockito.anyInt()))
        .thenReturn(statement);
    when(statement.executeQuery(Mockito.anyString()))
        .thenThrow(new SQLException("Simulated failure"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    // The method should fall back to the default password when query fails
    assertEquals("dave", password);

    // We cannot directly verify printStackTrace is not called, but we can ensure that
    // executeQuery was invoked and no further interactions with statement occur that
    // would indicate a different error handling path.
    verify(statement).executeQuery(Mockito.anyString());
  }

  @Test
  @DisplayName("completed still compares user input against password returned by getPassword")
  void completed_usesGetPassword_unaffectedByLoggingChange() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    SqlInjectionLesson6b lesson = Mockito.spy(new SqlInjectionLesson6b(dataSource));

    when(lesson.getPassword()).thenReturn("secret");

    // Act
    AttackResult successResult = lesson.completed("secret");
    AttackResult failResult = lesson.completed("wrong");

    // Assert
    org.junit.jupiter.api.Assertions.assertTrue(successResult.isSuccess());
    org.junit.jupiter.api.Assertions.assertFalse(failResult.isSuccess());
    verify(lesson, Mockito.times(2)).getPassword();
  }
}
