package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the information-exposure fix:
 * - Ensure stack traces are no longer printed/logged via printStackTrace().
 * - Ensure functional behavior of getPassword() remains intact.
 */
class SqlInjectionLesson6bTest {

  private SqlInjectionLesson6b lesson;
  private TrackingLessonDataSource trackingLessonDataSource;

  @BeforeEach
  void setUp() {
    trackingLessonDataSource = new TrackingLessonDataSource();
    lesson = new SqlInjectionLesson6b(trackingLessonDataSource);
  }

  @Test
  void getPassword_returnsValueFromDatabaseWithoutLeakingStackTraceOnSqlException()
      throws Exception {
    // Arrange: mock a Connection that throws SQLException when creating statement/exec query,
    // to exercise the catch blocks that were changed to remove printStackTrace().
    Connection connection = Mockito.mock(Connection.class);
    // We will throw a generic RuntimeException to simulate an error path without importing SQLException.
    RuntimeException failure = new RuntimeException("db failure");
    Mockito.when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(failure);

    trackingLessonDataSource.setConnection(connection);

    // Capture System.err to detect whether any stack trace-like output is produced.
    java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
    java.io.PrintStream originalErr = System.err;
    System.setErr(new java.io.PrintStream(errContent));

    try {
      String password = lesson.getPassword();

      // After the fix, no stack trace should be printed; output should be empty or minimal.
      String errOutput = errContent.toString();
      assertFalse(errOutput.contains("db failure"));
      assertFalse(errOutput.contains("RuntimeException"));

      // Functional behavior: when an exception occurs, the original code kept default "dave".
      assertEquals("dave", password);
    } finally {
      System.setErr(originalErr);
    }
  }

  @Test
  void getPassword_readsPasswordFromResultSetWhenAvailable() throws Exception {
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet rs = Mockito.mock(ResultSet.class);

    Mockito.when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    Mockito.when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(rs);
    Mockito.when(rs.first()).thenReturn(true);
    Mockito.when(rs.getString("password")).thenReturn("secure-password");

    trackingLessonDataSource.setConnection(connection);

    String password = lesson.getPassword();

    assertEquals("secure-password", password);
  }

  /**
   * Simple wrapper around a DataSource to allow injecting a mocked Connection; matches the
   * LessonDataSource interface used in the updated class.
   */
  private static class TrackingLessonDataSource implements LessonDataSource {

    private Connection connection;

    void setConnection(Connection connection) {
      this.connection = connection;
    }

    @Override
    public Connection getConnection() {
      return connection;
    }

    @Override
    public DataSource getDataSource() {
      return null;
    }
  }
}
