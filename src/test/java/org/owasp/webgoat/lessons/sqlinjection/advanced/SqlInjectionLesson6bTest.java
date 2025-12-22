// Derived from: src/main/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6b.java
// Test path (main -> test): src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests focused only on the changed behavior:
 * - Exception handling switched from printStackTrace() to structured SLF4J logging.
 * - Ensure no System.err stack traces are printed and Logger.error is used instead.
 */
class SqlInjectionLesson6bTest {

    @Test
    void getPassword_logsSqlExceptionWithLoggerInsteadOfPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
            .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("DB error"));

        // Mock the logger used inside SqlInjectionLesson6b
        Logger mockLogger = mock(Logger.class);

        try (MockedStatic<LoggerFactory> loggerFactoryMock = Mockito.mockStatic(LoggerFactory.class)) {
            loggerFactoryMock
                .when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                .thenReturn(mockLogger);

            SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

            // Act
            String password = lesson.getPassword();

            // Assert:
            // 1) default password is still returned on failure
            assertEquals("dave", password);

            // 2) ensure Logger.error was used to log the exception instead of printStackTrace()
            verify(mockLogger).error(
                eq("SQL Exception in getPassword: {}"),
                eq("DB error"),
                any(SQLException.class)
            );
        }
    }

    @Test
    void getPassword_logsGeneralExceptionWithLoggerInsteadOfPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        // Force an Exception from getConnection()
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection failed"));

        Logger mockLogger = mock(Logger.class);

        try (MockedStatic<LoggerFactory> loggerFactoryMock = Mockito.mockStatic(LoggerFactory.class)) {
            loggerFactoryMock
                .when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                .thenReturn(mockLogger);

            SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

            // Act
            String password = lesson.getPassword();

            // Assert:
            assertEquals("dave", password);

            verify(mockLogger).error(
                eq("General Exception in getPassword: {}"),
                eq("Connection failed"),
                any(RuntimeException.class)
            );
        }
    }
}
