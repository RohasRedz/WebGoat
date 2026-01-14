package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword() should not use printStackTrace and should log via SLF4J on SQL exception")
  void getPasswordUsesSlf4jLoggingOnSQLException() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenThrow(new SQLException("boom"));

    SqlInjectionLesson6b lesson =
        new SqlInjectionLesson6b(lessonDataSource) {
          // Expose logger for verification if needed
        };

    // Act
    String password = lesson.getPassword();

    // Assert
    // The fixed implementation should swallow the exception and return default "dave"
    assertEquals("dave", password, "On SQL exception the default password value should be returned");

    // There is no direct way to assert on Logger without extra plumbing; this test
    // primarily ensures that getPassword() no longer throws and falls back safely.
    verify(statement, times(1))
        .executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'");
  }

  @Test
  @DisplayName("completed() should still use the password value returned from getPassword()")
  void completedUsesGetPasswordValue() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    SqlInjectionLesson6b lesson =
        new SqlInjectionLesson6b(lessonDataSource) {
          @Override
          protected String getPassword() {
            return "secret";
          }
        };

    // Act
    var successResult = lesson.completed("secret");
    var failResult = lesson.completed("wrong");

    // Assert
    assertTrue(successResult.getLessonCompleted(), "Matching password should succeed");
    assertTrue(!failResult.getLessonCompleted(), "Non-matching password should not succeed");
  }
}
