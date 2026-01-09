package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on logging behavior after replacing
 * printStackTrace() with SLF4J log.error() calls.
 *
 * Resolved test path (from src/main/...):
 * src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
 */
public class SqlInjectionLesson6bTest {

  private LessonDataSource dataSource;
  private SqlInjectionLesson6b lesson;

  private final PrintStream originalErr = System.err;
  private ByteArrayOutputStream errContent;

  @BeforeEach
  void setUp() {
    dataSource = mock(LessonDataSource.class);
    lesson = new SqlInjectionLesson6b(dataSource);

    // Capture System.err to ensure no stack traces are printed anymore
    errContent = new ByteArrayOutputStream();
    System.setErr(new PrintStream(errContent));
  }

  @AfterEach
  void tearDown() {
    System.setErr(originalErr);
  }

  @Test
  void getPassword_returns_default_when_connection_fails_and_does_not_print_stacktrace() throws Exception {
    // Arrange: simulate a failure getting a connection
    when(dataSource.getConnection()).thenThrow(new RuntimeException("DB down"));

    // Act
    String password = lesson.getPassword();

    // Assert: behavior & security
    assertNotNull(password, "Password should not be null even when connection fails");
    assertEquals("dave", password, "Expected default password to be returned on error");

    String stderrOutput = errContent.toString();
    // We expect no Java stack trace patterns like 'at com.example.Class.method(' in stderr
    // because printStackTrace() has been removed and replaced by structured logging.
    org.junit.jupiter.api.Assertions.assertFalse(
        stderrOutput.contains("at "),
        "System.err should not contain stack trace lines after fix");
  }

  @Test
  void getPassword_handles_sql_exception_without_printing_stacktrace() throws Exception {
    // Arrange: simulate connection works but statement creation throws SQLException
    Connection connection = mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            Mockito.anyInt(),
            Mockito.anyInt()))
        .thenThrow(new SQLException("broken statement"));

    // Act
    String password = lesson.getPassword();

    // Assert
    assertNotNull(password, "Password should not be null when SQL exception occurs");
    assertEquals("dave", password, "Expected default password to be returned on SQL error");

    String stderrOutput = errContent.toString();
    org.junit.jupiter.api.Assertions.assertFalse(
        stderrOutput.contains("SQLException"),
        "System.err should not show raw SQL exception stack trace after fix");
    org.junit.jupiter.api.Assertions.assertFalse(
        stderrOutput.contains("at "),
        "System.err should not contain stack trace lines after fix");
  }
}
