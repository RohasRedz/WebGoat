package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on:
 * - Removal of hard-coded default password.
 * - Use of logging instead of printStackTrace in exception handling.
 *
 * Derived test path (per instructions):
 * src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
 */
@Slf4j
public class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword returns DB value when query succeeds (no hardcoded default)")
  void getPassword_returnsDatabasePassword_whenQuerySucceeds() throws Exception {
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
    when(resultSet.getString("password")).thenReturn("dbPassword");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals("dbPassword", password);
  }

  @Test
  @DisplayName("getPassword no longer falls back to hardcoded 'dave' on exception")
  void getPassword_returnsNull_whenQueryThrowsException() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new RuntimeException("DB failure"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    // After the fix, the method starts with password = null and does not use the old "dave" default.
    assertNull(password);

    // Also verify that createStatement was indeed invoked (exercise the exception path).
    verify(connection)
        .createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
  }
}
