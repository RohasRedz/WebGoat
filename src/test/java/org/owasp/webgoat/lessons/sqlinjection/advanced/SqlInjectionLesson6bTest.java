// TODO: Adjust package to match project structure if necessary.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging-related fix:
 * - getPassword() should still return the correct password from the DB.
 * - Exceptions must be logged through SLF4J logger, not via printStackTrace().
 */
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword should return password from database when query succeeds")
    void getPassword_returnsPasswordFromDatabase() throws Exception {
        // Arrange
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(lessonDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(lessonDataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("db-password", password, "getPassword should return the value retrieved from the database");
    }

    @Test
    @DisplayName("getPassword should log SQLExceptions via SLF4J logger instead of using printStackTrace")
    void getPassword_logsSQLExceptionWithSlf4jLogger() throws Exception {
        // Arrange
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(lessonDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(lessonDataSource);

        // Spy on the SLF4J logger used by the @Slf4j annotation.
        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock
                    .when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                    .thenReturn(logger);

            // Act
            String password = lesson.getPassword();

            // Assert
            // When an exception occurs, method should fall back to the default "dave" and log via logger.error(...)
            assertEquals("dave", password, "On SQL exception, getPassword should return the default password");
            verify(logger, atLeastOnce())
                    .error(eq("SQL Exception in getPassword method"), any(SQLException.class));
        }
    }

    @Test
    @DisplayName("getPassword should log generic Exceptions via SLF4J logger instead of using printStackTrace")
    void getPassword_logsGenericExceptionWithSlf4jLogger() throws Exception {
        // Arrange
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        // Force a generic exception by throwing a RuntimeException when acquiring connection
        when(lessonDataSource.getConnection()).thenThrow(new RuntimeException("Unexpected"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(lessonDataSource);

        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock
                    .when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                    .thenReturn(logger);

            // Act
            String password = lesson.getPassword();

            // Assert
            assertEquals("dave", password, "On generic exception, getPassword should return the default password");
            verify(logger, atLeastOnce())
                    .error(eq("Exception in getPassword method"), any(Exception.class));
        }
    }
}
