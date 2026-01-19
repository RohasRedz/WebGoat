/*
 * Unit tests for SqlInjectionLesson6b to validate secure logging behavior and exception handling.
 */

package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("completed uses getPassword and does not expose exceptions through uncontrolled stack traces")
  void completed_doesNotLeakStackTraceOnError() throws IOException, SQLException {
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);

    SqlInjectionLesson6b lesson = Mockito.spy(new SqlInjectionLesson6b(dataSource));

    Mockito.doThrow(new RuntimeException("Test exception")).when(lesson).getPassword();

    AttackResult result = lesson.completed("anyUser");

    assertEquals(false, result.isLessonCompleted());

    Mockito.verify(connection, never())
        .createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
  }

  @Test
  @DisplayName("getPassword swallows SQLExceptions without propagating stack traces")
  void getPassword_handlesSqlExceptionWithoutPrintStackTrace() throws Exception {
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);

    Statement statement = Mockito.mock(Statement.class);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery(Mockito.anyString())).thenThrow(new SQLException("DB error"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    String password = lesson.getPassword();

    assertEquals("dave", password);
  }
}
