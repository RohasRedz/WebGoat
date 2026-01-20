package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword() should read password from database without leaking stack traces")
  void getPasswordReadsFromDatabaseWithoutPrintStackTrace() throws Exception {
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
    when(resultSet.getString("password")).thenReturn("db-password");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals("db-password", password);
    Mockito.verifyNoMoreInteractions(Mockito.mock(Throwable.class));
  }

  @Test
  @DisplayName("completed() should succeed when userid_6b matches the resolved password")
  void completedSucceedsWhenUserIdMatchesPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    SqlInjectionLesson6b lesson =
        new SqlInjectionLesson6b(dataSource) {
          @Override
          protected String getPassword() {
            return "secret-pass";
          }
        };

    // Act
    AttackResult successResult = lesson.completed("secret-pass");
    AttackResult failResult = lesson.completed("other");

    // Assert
    assertEquals(AttackResult.Status.SUCCESS, successResult.getLessonStatus());
    assertEquals(AttackResult.Status.FAIL, failResult.getLessonStatus());
  }

  @Test
  @DisplayName("getPassword() should return default password when any exception occurs without throwing")
  void getPasswordSwallowsExceptionsWithoutLoggingStackTrace() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    when(dataSource.getConnection()).thenThrow(new RuntimeException("DB not available"));
    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    assertNotNull(password);
    assertEquals("dave", password);
  }
}
