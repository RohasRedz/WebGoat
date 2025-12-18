package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging change:
 * ensuring exceptions are routed through SLF4J logger instead of printStackTrace.
 */
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword logs SQLExceptions via SLF4J logger instead of printStackTrace")
    void getPasswordLogsSQLExceptionViaLogger() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new java.sql.SQLException("DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Spy on logger used by the class under test.
        Logger logger = LoggerFactory.getLogger(SqlInjectionLesson6b.class);
        Logger spyLogger = spy(logger);

        // Replace the static logger instance via reflection so we can verify calls.
        // NOTE: This uses reflection solely for delta verification of the logging behavior.
        var logField = SqlInjectionLesson6b.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(null, spyLogger);

        // Act
        assertDoesNotThrow(lesson::getPassword);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(spyLogger).error(messageCaptor.capture(), anyString(), any(Throwable.class));

        String loggedMessage = messageCaptor.getValue();
        // Delta assertion: a structured error message should be logged instead of printStackTrace.
        // We don't assert the exact wording, only that logging occurred.
        // e.g., "Error fetching password from database: {}"
        // This confirms that the catch block uses logger, not e.printStackTrace().
        // (If printStackTrace were still used, this verification would fail.)
        org.junit.jupiter.api.Assertions.assertTrue(
                loggedMessage.toLowerCase().contains("error"),
                "Expected an error-level log message when SQLException occurs");
    }

    @Test
    @DisplayName("getPassword logs unexpected Exceptions via SLF4J logger")
    void getPasswordLogsUnexpectedExceptionsViaLogger() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection failure"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Logger logger = LoggerFactory.getLogger(SqlInjectionLesson6b.class);
        Logger spyLogger = spy(logger);

        var logField = SqlInjectionLesson6b.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(null, spyLogger);

        // Act
        assertDoesNotThrow(lesson::getPassword);

        // Assert
        verify(spyLogger).error(
                eq("Unexpected error in getPassword method: {}"),
                anyString(),
                any(Throwable.class));
    }
}
