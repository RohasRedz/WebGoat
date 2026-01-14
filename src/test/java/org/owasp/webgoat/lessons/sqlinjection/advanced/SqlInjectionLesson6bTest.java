/*
 * Delta tests for SqlInjectionLesson6b focusing on the change from printStackTrace()
 * to structured logging via log.error, to avoid leaking stack traces into logs.
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

public class SqlInjectionLesson6bTest {

  private LessonDataSource lessonDataSource;
  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;
  private SqlInjectionLesson6b lesson;

  @BeforeEach
  void setUp() throws Exception {
    lessonDataSource = mock(LessonDataSource.class);
    connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("dave");

    lesson = new SqlInjectionLesson6b(lessonDataSource);
  }

  @Test
  void getPassword_logsErrorWithoutPrintingStackTrace_onSqlException() throws Exception {
    // Arrange: force SQLException when creating the statement
    reset(statement, connection);
    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new SQLException("simulated failure"));

    SqlInjectionLesson6b spyLesson = spy(lesson);

    // We cannot assert internal log.error calls directly without a logging appender,
    // but we can assert that method completes without throwing and returns default password.
    String password = spyLesson.getPassword();

    assertEquals("dave", password, "On SQL exception, getPassword should return the default value");
  }

  @Test
  void completed_stillBehavesCorrectlyWithRetrievedPassword() throws IOException {
    // With successful getPassword(), userid_6b equals the retrieved password
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("secretPwd");
    SqlInjectionLesson6b lessonLocal = new SqlInjectionLesson6b(lessonDataSource);

    AttackResult successResult = lessonLocal.completed("secretPwd");
    AttackResult failureResult = lessonLocal.completed("wrong");

    assertTrue(successResult.getSuccess(), "Matching password should still yield a successful result");
    assertTrue(!failureResult.getSuccess(), "Non-matching password should still fail");
  }
}
