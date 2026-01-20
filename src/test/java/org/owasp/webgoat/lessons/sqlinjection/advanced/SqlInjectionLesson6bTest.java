package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the secure logging fix:
 * - Verifies that SQL and general exceptions are logged with generic messages.
 * - Verifies no sensitive details (like the query) are formatted into the log message text.
 */
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword() should log generic message for SQL exceptions without exposing sensitive query details")
    void getPassword_logsGenericMessageForSqlException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);

        SQLExceptionForTest sqlException = new SQLExceptionForTest("synthetic SQL error");
        when(statement.executeQuery(anyString())).thenThrow(sqlException);

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Capture logs via Logback test logger
        Logger logger = (Logger) LoggerFactory.getLogger(SqlInjectionLesson6b.class);
        TestLogAppender appender = new TestLogAppender();
        appender.setContext(logger.getLoggerContext());
        logger.addAppender(appender);
        logger.setLevel(Level.ERROR);
        appender.start();

        // Act
        String password = lesson.getPassword();

        // Assert: password falls back to default value due to error
        assertEquals("dave", password, "On SQL error, password should remain default");

        // Assert: exactly one error log with generic message is produced
        assertEquals(1, appender.getEvents().size(), "Expected a single error log event");
        String logMessage = appender.getEvents().get(0).getFormattedMessage();

        // The fixed code uses generic messages; it should not contain SQL text or table names.
        // It should match the updated message string from the source.
        // We also assert that it does not contain 'user_system_data' or 'SELECT password'.
        org.junit.jupiter.api.Assertions.assertTrue(
                logMessage.contains("An SQL error occurred during password retrieval."),
                "Log should use a generic error message");
        org.junit.jupiter.api.Assertions.assertFalse(
                logMessage.contains("user_system_data") || logMessage.contains("SELECT password"),
                "Log message should not expose SQL query or table details");

        // Verify that the exception object was attached to the log (so stack trace is available internally)
        Throwable thrown = appender.getEvents().get(0).getThrowableProxy() != null
                ? appender.getEvents().get(0).getThrowableProxy().getThrowable()
                : null;
        org.junit.jupiter.api.Assertions.assertSame(sqlException, thrown,
                "Logged exception should be the thrown SQL exception instance");
    }

    // Helper SQLException subclass for clarity in tests
    private static class SQLExceptionForTest extends java.sql.SQLException {
        SQLExceptionForTest(String message) {
            super(message);
        }
    }

    /**
     * Simple Logback appender to capture log events for assertions.
     */
    private static class TestLogAppender extends ch.qos.logback.core.AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent> {
        private final java.util.List<ch.qos.logback.classic.spi.ILoggingEvent> events = new java.util.ArrayList<>();

        @Override
        protected void append(ch.qos.logback.classic.spi.ILoggingEvent eventObject) {
            events.add(eventObject);
        }

        java.util.List<ch.qos.logback.classic.spi.ILoggingEvent> getEvents() {
            return events;
        }
    }
}
