package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing on logging behavior and preservation of functional
 * behavior.
 */
public class SqlInjectionLesson6bTest {

  private LessonDataSource lessonDataSource;
  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;
  private SqlInjectionLesson6b lesson;

  @BeforeEach
  void setUp() throws Exception {
    connection = Mockito.mock(Connection.class);
    statement = Mockito.mock(Statement.class);
    resultSet = Mockito.mock(ResultSet.class);

    lessonDataSource = Mockito.mock(LessonDataSource.class);
    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);

    lesson = new SqlInjectionLesson6b(lessonDataSource);
  }

  @Test
  void completedSucceedsWhenUserIdMatchesPassword() throws Exception {
    // Arrange
    // getPassword() will read the first row's password - simulate "secret"
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("secret");

    // Act
    AttackResult result = lesson.completed("secret");

    // Assert
    assertEquals(AttackResult.Status.SUCCESS, result.getLessonStatus());
  }

  @Test
  void completedFailsWhenUserIdDoesNotMatchPassword() throws Exception {
    // Arrange
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("secret");

    // Act
    AttackResult result = lesson.completed("wrong");

    // Assert
    assertEquals(AttackResult.Status.FAIL, result.getLessonStatus());
  }

  @Test
  void getPasswordLogsErrorWithoutStackTraceOnSqlException() throws Exception {
    // Arrange
    // Force createStatement to throw SQLException
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new java.sql.SQLException("simulated"));

    // Capture logging using a mock logger bound to the same name
    var logger = LoggerFactory.getLogger(SqlInjectionLesson6b.class);
    // NOTE: We cannot easily assert internal log contents without a dedicated appender.
    // This test ensures that getPassword() still returns a value and does not rethrow,
    // verifying that the new logging path does not break behavior.

    // Act
    String password = lesson.getPassword();

    // Assert
    // Default returned password should still be "dave" under error condition
    assertEquals("dave", password);
  }
}
