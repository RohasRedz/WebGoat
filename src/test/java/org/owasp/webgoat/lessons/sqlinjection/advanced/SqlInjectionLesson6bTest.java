package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlInjectionLesson6bTest {

  private LessonDataSource dataSource;
  private SqlInjectionLesson6b lesson6b;

  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = mock(LessonDataSource.class);
    lesson6b = new SqlInjectionLesson6b(dataSource);

    connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("dave");
  }

  @Test
  void getPassword_returnsNonNullValue_whenQuerySucceeds() {
    String password = lesson6b.getPassword();

    assertNotNull(password);
    assertEquals("dave", password);
  }

  @Test
  void getPassword_returnsDefaultAndDoesNotThrow_whenSqlExceptionOccurs() throws Exception {
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new SQLException("synthetic SQL error"));

    String password = lesson6b.getPassword();

    assertNotNull(password);
    assertEquals("dave", password);
  }

  @Test
  void getPassword_logsErrorWhenSqlExceptionOccurs() throws Exception {
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new SQLException("synthetic SQL error"));

    Logger logger = LoggerFactory.getLogger(SqlInjectionLesson6b.class);
    TestLogAppender appender = new TestLogAppender();
    appender.start();
    ((ch.qos.logback.classic.Logger) logger).addAppender(appender);

    try {
      lesson6b.getPassword();

      boolean hasErrorLog =
          appender.getEvents().stream()
              .anyMatch(
                  e ->
                      e.getLevel() == ch.qos.logback.classic.Level.ERROR
                          && e.getFormattedMessage()
                              .contains(
                                  "SQL error during password retrieval for user 'dave'"));
      org.junit.jupiter.api.Assertions.assertTrue(hasErrorLog);
    } finally {
      ((ch.qos.logback.classic.Logger) logger).detachAppender(appender);
      appender.stop();
    }
  }

  private static class TestLogAppender
      extends ch.qos.logback.core.read.ListAppender<
          ch.qos.logback.classic.spi.ILoggingEvent> {
    java.util.List<ch.qos.logback.classic.spi.ILoggingEvent> getEvents() {
      return this.list;
    }
  }
}
