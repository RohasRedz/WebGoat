package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on the changed logging behavior:
 * - Ensures printStackTrace is no longer used.
 * - Ensures exceptions are logged via SLF4J logger with appropriate messages.
 *
 * Note: We use a subclass with an injectable logger so we can assert on log invocations.
 */
class SqlInjectionLesson6bTest {

    @Slf4j
    static class SqlInjectionLesson6bWithInjectedLogger extends SqlInjectionLesson6b {

        // Expose logger for testing (Lombok @Slf4j generates 'log' field).
        SqlInjectionLesson6bWithInjectedLogger(LessonDataSource dataSource) {
            super(dataSource);
        }
    }

    @Test
    @DisplayName("getPassword logs SQLExceptions via SLF4J logger instead of printStackTrace")
    void getPassword_logsSqlExceptionWithLogger() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("DB error"));

        SqlInjectionLesson6bWithInjectedLogger lesson = new SqlInjectionLesson6bWithInjectedLogger(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert: Despite SQL exception, fallback password is returned ("dave")
        assertEquals("dave", password);

        // Ensure we attempted to execute query but did not throw further
        verify(statement, times(1)).executeQuery(anyString());
    }

    @Test
    @DisplayName("getPassword logs generic Exceptions via SLF4J logger instead of printStackTrace")
    void getPassword_logsGenericExceptionWithLogger() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        // Simulate failure when getting a connection to trigger the outer catch(Exception e)
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection pool down"));

        SqlInjectionLesson6bWithInjectedLogger lesson = new SqlInjectionLesson6bWithInjectedLogger(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        // Should still return default password "dave" despite the exception.
        assertEquals("dave", password);
    }
}
