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

public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword logs SQL exceptions via logger instead of printStackTrace")
    void getPassword_logsSqlExceptionWithoutPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("boom"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Spy logger to ensure log.error is called (delta behavior)
        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock.when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                    .thenReturn(logger);

            // Act
            String password = lesson.getPassword();

            // Assert
            // Default password should be returned (existing behavior)
            assertEquals("dave", password);
            // New behavior: error is logged via SLF4J instead of using printStackTrace
            verify(logger).error(startsWith("SQL Exception in getPassword"), anyString());
        }
    }

    @Test
    @DisplayName("getPassword logs general exceptions via logger instead of printStackTrace")
    void getPassword_logsGeneralExceptionWithoutPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new RuntimeException("runtime-err"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock.when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                    .thenReturn(logger);

            // Act
            String password = lesson.getPassword();

            // Assert
            assertEquals("dave", password);
            verify(logger).error(startsWith("General Exception in getPassword"), anyString());
        }
    }
}
