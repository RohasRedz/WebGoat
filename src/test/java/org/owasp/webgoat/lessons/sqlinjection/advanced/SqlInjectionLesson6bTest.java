package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing on changed behavior:
 * - Exceptions are now logged via SLF4J log.error instead of printStackTrace.
 * - getPassword() should still handle errors without propagating exceptions.
 */
class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    // We will intercept the generated SLF4J logger by mocking LoggerFactory.getLogger(...)
    private Logger loggerMock;
    private MockedStatic<LoggerFactory> loggerFactoryMock;

    @BeforeEach
    void setUp() {
        dataSource = Mockito.mock(LessonDataSource.class);
        lesson = new SqlInjectionLesson6b(dataSource);

        loggerMock = Mockito.mock(Logger.class);
        loggerFactoryMock = Mockito.mockStatic(LoggerFactory.class);
        // Return our mock logger whenever LoggerFactory.getLogger(...) is called
        loggerFactoryMock.when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                .thenReturn(loggerMock);
    }

    @Test
    void getPassword_logsSqlExceptionWithLogErrorAndDoesNotThrow() throws Exception {
        // Arrange: dataSource.getConnection() returns a connection whose createStatement throws SQLException
        Connection connection = Mockito.mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("DB error"));

        // Act: method should catch the SQLException, log via log.error, and return the default password
        String password = lesson.getPassword();

        // Assert: default password 'dave' is still returned and no exception is propagated
        org.junit.jupiter.api.Assertions.assertEquals("dave", password);

        // Verify that log.error was called with the expected message pattern
        verify(loggerMock, times(1))
                .error(Mockito.eq("SQL Exception occurred while retrieving password: {}"),
                       Mockito.eq("DB error"));
    }

    @Test
    void getPassword_logsGenericExceptionWithLogErrorAndDoesNotThrow() throws Exception {
        // Arrange: dataSource.getConnection() itself throws a generic Exception
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection failed"));

        // Act
        String password = lesson.getPassword();

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals("dave", password);

        // Verify logging via log.error for general exception path
        verify(loggerMock, times(1))
                .error(Mockito.eq("General Exception occurred while retrieving password: {}"),
                       Mockito.eq("Connection failed"));
    }
}
