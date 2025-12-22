// Assuming standard package based on resolved_file_path from the workflow.
// If actual package differs, adjust accordingly.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on the changed behavior:
 * - stack traces must no longer be printed to logs via printStackTrace().
 */
public class SqlInjectionLesson6bTest {

  private LessonDataSource lessonDataSource;
  private SqlInjectionLesson6b lesson6b;

  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    lessonDataSource = mock(LessonDataSource.class);
    connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);

    org.mockito.Mockito.when(lessonDataSource.getConnection()).thenReturn(connection);
    org.mockito.Mockito.when(
            connection.createStatement(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(statement);
    org.mockito.Mockito.when(statement.executeQuery(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(resultSet);

    lesson6b = new SqlInjectionLesson6b(lessonDataSource);
  }

  @Test
  void getPassword_doesNotPrintStackTraceOnSqlException() throws Exception {
    // Arrange: make createStatement throw SQLException
    SQLException sqlException = new SQLException("DB error");
    org.mockito.Mockito.when(
            connection.createStatement(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
        .thenThrow(sqlException);

    // Spy on the exception object to ensure printStackTrace is never invoked.
    SQLException spyException = org.mockito.Mockito.spy(sqlException);

    // We need connection to throw our spy instead of the original.
    org.mockito.Mockito.when(
            connection.createStatement(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
        .thenThrow(spyException);

    // Act
    String password = lesson6b.getPassword();

    // Assert: default password is still returned, but no stack trace printing occurs
    assertEquals("dave", password, "Default password should be returned on error");

    // Ensure printStackTrace was never called on the thrown SQLException
    verify(spyException, never()).printStackTrace();
  }

  @Test
  void getPassword_doesNotPrintStackTraceOnGenericException() throws Exception {
    // Arrange: make getConnection throw a generic Exception
    Exception generic = new Exception("generic");
    Exception spyGeneric = org.mockito.Mockito.spy(generic);
    org.mockito.Mockito.when(lessonDataSource.getConnection()).thenThrow(spyGeneric);

    // Act
    String password = lesson6b.getPassword();

    // Assert: default password is still returned, but no stack trace printing occurs
    assertEquals("dave", password, "Default password should be returned on generic error");

    // Ensure printStackTrace was never called on the thrown Exception
    verify(spyGeneric, never()).printStackTrace();
  }
}
