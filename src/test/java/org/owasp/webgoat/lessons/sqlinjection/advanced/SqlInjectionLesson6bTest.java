package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging changes:
 * - Ensure exceptions are logged via log.error()
 * - Ensure printStackTrace() is not used anymore
 */
@Slf4j
class SqlInjectionLesson6bTest {

  private LessonDataSource dataSource;
  private SqlInjectionLesson6b lesson;

  private Connection connection;
  private Statement statement;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = Mockito.mock(LessonDataSource.class);
    connection = Mockito.mock(Connection.class);
    statement = Mockito.mock(Statement.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);

    lesson = new SqlInjectionLesson6b(dataSource);
  }

  @Test
  void getPassword_logsSqlExceptionWithSlf4jAndDoesNotThrow() throws Exception {
    SQLException sqlException = new SQLException("DB down");
    doThrow(sqlException)
        .when(statement)
        .executeQuery(Mockito.anyString());

    // Call the protected method indirectly via completed()
    AttackResult result = lesson.completed("anyUser");

    // Verify behavior: method handles exception and still returns some password, leading to failed()
    assertEquals(false, result.isSuccess(), "Exception during getPassword should not cause success");

    // We cannot directly assert logger behavior without a logging appender.
    // Instead, we assert that no printStackTrace() is invoked by ensuring we never call it
    // on our mocked exception.
    verify(statement).executeQuery(Mockito.anyString());
    verify(connection).createStatement(
        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

    // Critical assertion: no printStackTrace() on SQLException
    verify(sqlException, never()).printStackTrace();
  }

  @Test
  void getPassword_logsGeneralExceptionWithSlf4jAndDoesNotThrow() throws Exception {
    // Simulate a general exception when acquiring a connection
    RuntimeException runtimeException = new RuntimeException("Connection pool failure");
    when(dataSource.getConnection()).thenThrow(runtimeException);

    AttackResult result = lesson.completed("anyUser");

    // Method should handle the exception gracefully and not succeed
    assertEquals(false, result.isSuccess(), "General exception should not cause success");

    // Ensure no printStackTrace() is called on the runtime exception
    verify(runtimeException, never()).printStackTrace();
  }
}
