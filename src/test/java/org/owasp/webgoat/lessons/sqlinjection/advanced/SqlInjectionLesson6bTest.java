// File: src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.LoggerFactory;

class SqlInjectionLesson6bTest {

  @Test
  void getPassword_logsErrorInsteadOfPrintingStackTraceOnSqlException() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenThrow(new SQLException("boom"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    Logger logger = (Logger) LoggerFactory.getLogger(SqlInjectionLesson6b.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    lesson.getPassword();

    boolean hasSqlErrorLog =
        listAppender.list.stream()
            .anyMatch(
                e ->
                    e.getLevel().equals(Level.ERROR)
                        && e.getFormattedMessage()
                            .contains("SQL Exception occurred during password retrieval"));
    assertTrue(
        hasSqlErrorLog,
        "Expected an ERROR log entry for SQL exception instead of stack trace printing");
  }

  @Test
  void getPassword_logsErrorOnGenericException() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);

    when(dataSource.getConnection()).thenThrow(new RuntimeException("connection-failure"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    Logger logger = (Logger) LoggerFactory.getLogger(SqlInjectionLesson6b.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    lesson.getPassword();

    boolean hasGenericErrorLog =
        listAppender.list.stream()
            .anyMatch(
                e ->
                    e.getLevel().equals(Level.ERROR)
                        && e.getFormattedMessage()
                            .contains("An unexpected error occurred during password retrieval"));
    assertTrue(
        hasGenericErrorLog,
        "Expected an ERROR log entry for generic exception instead of stack trace printing");
  }
}
