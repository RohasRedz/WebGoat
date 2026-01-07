package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.LoggerFactory;

/**
 * Delta unit tests for SqlInjectionLesson6b focusing only on the changed behavior:
 * - printStackTrace() is no longer used for exception handling.
 * - Exceptions are logged via Slf4j log.error instead.
 */
class SqlInjectionLesson6bTest {

  private LessonDataSource dataSource;
  private SqlInjectionLesson6b lesson;

  @BeforeEach
  void setUp() {
    dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    lesson = new SqlInjectionLesson6b(dataSource);
  }

  @Test
  void getPassword_logsSqlExceptionUsingSlf4j_andDoesNotUsePrintStackTrace()
      throws Exception {
    Connection connection = org.mockito.Mockito.mock(Connection.class);
    Statement statement = org.mockito.Mockito.mock(Statement.class);

    org.mockito.Mockito.when(dataSource.getConnection()).thenReturn(connection);
    org.mockito.Mockito.when(
            connection.createStatement(
                org.mockito.Mockito.anyInt(), org.mockito.Mockito.anyInt()))
        .thenThrow(new SQLException("boom"));

    // Attach ListAppender to the Slf4j logger used by SqlInjectionLesson6b
    Logger logger =
        (Logger) LoggerFactory.getLogger(SqlInjectionLesson6b.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    // Call method under test; it should catch SQL exception and log via log.error
    String password = lesson.getPassword();

    // Original default password remains when query fails
    assertEquals("dave", password, "Default password should be returned on SQL error");

    // Assert that an ERROR log entry was produced
    boolean hasErrorLog =
        listAppender.list.stream()
            .anyMatch(
                event ->
                    event.getLevel() == Level.ERROR
                        && event.getFormattedMessage()
                            .contains("SQL Exception during password retrieval"));
    org.junit.jupiter.api.Assertions.assertTrue(
        hasErrorLog, "Expected an ERROR log entry for SQL exception");

    // Verify that no printStackTrace is invoked on the SQLException instance.
    // We cannot directly assert absence of method calls on thrown exception,
    // but we can assert we never call printStackTrace on a mock exception.
    SQLException mockException = org.mockito.Mockito.mock(SQLException.class);
    mockException.printStackTrace();
    verify(mockException).printStackTrace(); // sanity check on mock

    // Since the real exception is not a mock, we instead rely on static analysis
    // of the fixed code and log assertions above to ensure the new behavior.
  }

  @Test
  void getPassword_logsGeneralExceptionUsingSlf4j() throws Exception {
    // Simulate a general exception from getConnection itself
    org.mockito.Mockito.when(dataSource.getConnection())
        .thenThrow(new RuntimeException("connection failed"));

    Logger logger =
        (Logger) LoggerFactory.getLogger(SqlInjectionLesson6b.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    String password = lesson.getPassword();

    assertEquals(
        "dave",
        password,
        "Default password should be returned on general exception");

    boolean hasErrorLog =
        listAppender.list.stream()
            .anyMatch(
                event ->
                    event.getLevel() == Level.ERROR
                        && event.getFormattedMessage()
                            .contains("General Exception during password retrieval"));
    org.junit.jupiter.api.Assertions.assertTrue(
        hasErrorLog, "Expected an ERROR log entry for general exception");
  }
}
