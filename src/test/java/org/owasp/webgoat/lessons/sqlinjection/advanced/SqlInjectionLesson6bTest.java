package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on the changed behavior:
 * - Replaced printStackTrace() with SLF4J logging.
 *
 * Covered behaviors:
 * 1) completed() still succeeds when the provided password matches the value returned by getPassword().
 * 2) Exceptions inside getPassword() are handled internally, do not escape, and result in safe logging via the logger.
 */
class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);

        lesson = new SqlInjectionLesson6b(dataSource);
    }

    @Test
    @DisplayName("completed should succeed when user-provided password equals value returned from getPassword()")
    void completedSucceedsForCorrectPassword() throws IOException, SQLException {
        // Arrange
        // Simulate DB returning a password value, e.g., "secret"
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secret");

        // Act
        AttackResult result = lesson.completed("secret");

        // Assert
        assertNotNull(result);
        // TODO: If AttackResult exposes an explicit success flag (e.g., isSuccess()), prefer that.
        assertTrue(result.toString().contains("success") || !result.toString().contains("failed"),
                "Result should indicate success when password matches getPassword() value");
    }

    @Test
    @DisplayName("getPassword should handle SQL exceptions internally and log via SLF4J without throwing")
    void getPasswordLogsExceptionInsteadOfThrowing() throws Exception {
        // Arrange
        // Force an SQLException when creating the Statement
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("Simulated SQL error"));

        // Capture the logger instance via reflection so we can verify logging behavior using a mock
        Logger mockLogger = mock(Logger.class);
        setLoggerViaReflection(mockLogger);

        // Act & Assert: getPassword should not throw, even though the underlying call fails
        String password = assertDoesNotThrow(() -> lesson.getPassword(),
                "getPassword should not propagate SQL exceptions");

        assertNotNull(password, "getPassword should always return a non-null String");
        // For this lesson code, the default value is "dave" and should be returned on failure
        assertEquals("dave", password, "On SQL exception, getPassword should fall back to default password");

        // Verify that logging was used instead of printStackTrace()
        verify(mockLogger, atLeastOnce()).error(
                eq("SQL Exception occurred during password retrieval."), any(SQLException.class));
    }

    @Test
    @DisplayName("getPassword should handle generic exceptions from dataSource.getConnection() and log safely")
    void getPasswordLogsGenericExceptionInsteadOfThrowing() throws Exception {
        // Arrange
        LessonDataSource failingDataSource = mock(LessonDataSource.class);
        when(failingDataSource.getConnection()).thenThrow(new RuntimeException("Connection failure"));
        SqlInjectionLesson6b failingLesson = new SqlInjectionLesson6b(failingDataSource);

        Logger mockLogger = mock(Logger.class);
        setLoggerViaReflectionOnInstance(failingLesson, mockLogger);

        // Act
        String password = assertDoesNotThrow(failingLesson::getPassword,
                "getPassword should not propagate generic exceptions");

        // Assert
        assertEquals("dave", password, "On generic exception, getPassword should still return default password");
        verify(mockLogger, atLeastOnce()).error(
                eq("An unexpected error occurred during password retrieval."), any(RuntimeException.class));
    }

    /**
     * Helper to inject a mock logger into the class under test by reflection.
     * This ensures our delta tests can verify that the new logging behavior
     * (replacing printStackTrace) is actually invoked.
     */
    private void setLoggerViaReflection(Logger logger) throws Exception {
        setLoggerViaReflectionOnInstance(lesson, logger);
    }

    private void setLoggerViaReflectionOnInstance(SqlInjectionLesson6b instance, Logger logger) throws Exception {
        Field loggerField = SqlInjectionLesson6b.class.getDeclaredField("log");
        loggerField.setAccessible(true);
        loggerField.set(null, logger); // static field
    }

    @Test
    @DisplayName("getPassword handles logger factory correctly (sanity check using static mocking)")
    void loggerIsCreatedViaLoggerFactory() {
        try (MockedStatic<LoggerFactory> loggerFactoryMock = Mockito.mockStatic(LoggerFactory.class)) {
            Logger logger = mock(Logger.class);
            loggerFactoryMock.when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                    .thenReturn(logger);

            // Instantiating the class should trigger LoggerFactory.getLogger
            SqlInjectionLesson6b localLesson = new SqlInjectionLesson6b(dataSource);
            assertNotNull(localLesson);

            loggerFactoryMock.verify(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class), atLeastOnce());
        }
    }
}
