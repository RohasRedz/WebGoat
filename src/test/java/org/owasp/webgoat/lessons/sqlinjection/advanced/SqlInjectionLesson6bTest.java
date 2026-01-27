package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the updated getPassword() behavior:
 * 1) It still prefers the password read from the database when available.
 * 2) It only falls back to the new non-sensitive demo default when the DB does not return a row.
 */
public class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword returns value from database when query yields a row")
  void getPassword_usesDatabaseValueWhenAvailable() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    // This is the value that should override the demo default password.
    String dbPassword = "db-secret";

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn(dbPassword);

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String result = lesson.getPassword();

    // Assert
    // When DB row exists, the returned password must come from the database,
    // not from the demo default literal.
    assertEquals(dbPassword, result);
  }

  @Test
  @DisplayName("getPassword falls back to demo default when database does not return a row")
  void getPassword_usesDemoDefaultWhenNoDatabaseRow() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    // Simulate: no rows in the result set
    when(resultSet.first()).thenReturn(false);

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String result = lesson.getPassword();

    // Assert
    // When DB does not provide a password, the method must return the new
    // non-sensitive demo default literal introduced by the fix.
    assertEquals("demo_password_for_lesson_only", result);
  }
}
