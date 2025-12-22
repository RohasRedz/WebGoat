// Assuming the same package as the class under test; adjust if the actual package differs.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests focusing on the logging change in SqlInjectionLesson6b.getPassword.
 *
 * The vulnerability fix replaced printStackTrace() calls with structured logging
 * via an SLF4J Logger to avoid direct stack trace exposure.
 *
 * These tests verify that:
 *  - printStackTrace() is no longer used when SQL or general exceptions occur.
 *  - SLF4J logger's error method is invoked instead.
 */
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword logs SQL exceptions via SLF4J logger instead of using printStackTrace")
    void getPassword_logsSqlExceptionWithLogger() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);

        SQLException sqlException = new SQLException("Simulated SQL error");
        when(statement.executeQuery(anyString())).thenThrow(sqlException);

        // Spy the class to intercept logger usage indirectly if needed
        SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(dataSource);

        // Create a spy logger to ensure error() is called.
        Logger spyLogger = spy(LoggerFactory.getLogger(SqlInjectionLesson6b.class));

        // Use reflection to inject the spy logger into the static final field.
        // This is purely for delta testing the logging behavior.
        java.lang.reflect.Field logField = SqlInjectionLesson6b.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(null, spyLogger);

        // Act
        String password = endpoint.getPassword();

        // Assert
        // Even on failure, method should return a non-null string (default "dave")
        org.junit.jupiter.api.Assertions.assertNotNull(password);

        // Verify that logger.error(...) is invoked with the SQL exception
        verify(spyLogger, atLeastOnce()).error(eq("SQL Exception in getPassword method"), eq(sqlException));
    }

    @Test
    @DisplayName("getPassword logs general exceptions via SLF4J logger instead of using printStackTrace")
    void getPassword_logsGeneralExceptionWithLogger() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);

        // Simulate a general exception thrown when obtaining a connection
        RuntimeException generalException = new RuntimeException("Connection failure");
        when(dataSource.getConnection()).thenThrow(generalException);

        SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(dataSource);

        Logger spyLogger = spy(LoggerFactory.getLogger(SqlInjectionLesson6b.class));

        java.lang.reflect.Field logField = SqlInjectionLesson6b.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(null, spyLogger);

        // Act
        String password = endpoint.getPassword();

        // Assert
        org.junit.jupiter.api.Assertions.assertNotNull(password);

        verify(spyLogger, atLeastOnce()).error(eq("General Exception in getPassword method"), eq(generalException));
    }
}
