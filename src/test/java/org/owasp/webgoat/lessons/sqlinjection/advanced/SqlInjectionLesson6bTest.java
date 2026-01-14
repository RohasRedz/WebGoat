package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on the logging change that
 * replaced printStackTrace with SLF4J logging to avoid direct stack trace exposure.
 */
public class SqlInjectionLesson6bTest {

  private final PrintStream originalErr = System.err;
  private ByteArrayOutputStream errContent;

  @BeforeEach
  void setUpStreams() {
    errContent = new ByteArrayOutputStream();
    System.setErr(new PrintStream(errContent));
  }

  @AfterEach
  void restoreStreams() {
    System.setErr(originalErr);
  }

  @Test
  @DisplayName("getPassword() should not use printStackTrace() and must avoid dumping stack trace to stderr")
  void getPassword_doesNotPrintStackTraceToStdErr() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito
        .when(connection.createStatement(
            Mockito.anyInt(),
            Mockito.anyInt()))
        .thenReturn(statement);
    Mockito.when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
    Mockito.when(resultSet.first()).thenThrow(new RuntimeException("Simulated DB failure"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    // Any exception thrown inside getPassword should now be logged via SLF4J
    // and not print its stack trace to System.err
    lesson.getPassword();

    // Assert
    String stderrOutput = errContent.toString();
    // The delta behavior we care about: printStackTrace writes class name/stack lines.
    // We assert that no stack trace-like line was printed.
    boolean containsStackTraceMarker =
        stderrOutput.contains("RuntimeException")
            || stderrOutput.contains("at org.owasp.webgoat");
    assertTrue(
        !containsStackTraceMarker,
        "Stack trace should not be printed to stderr; logging must be used instead");
  }
}
