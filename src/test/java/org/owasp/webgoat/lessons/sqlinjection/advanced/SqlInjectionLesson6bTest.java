// Batch 3 - Derived test path: src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;

/**
 * Delta tests for SqlInjectionLesson6b focusing on:
 * - getPassword still returns DB password when query succeeds.
 * - Exceptions are logged via log.error and do not propagate.
 */
public class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        lesson = new SqlInjectionLesson6b(dataSource);

        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                anyInt(),
                anyInt()))
            .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
    }

    @Test
    void getPassword_returnsPasswordFromDatabaseOnSuccess() throws Exception {
        // Arrange
        when(resultSet != null && resultSet.first()).thenReturn(true);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("db-password", password, "Should return password from DB when query succeeds");

        // Also ensure the correct query is executed (unchanged logic)
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement).executeQuery(queryCaptor.capture());
        assertEquals(
                "SELECT password FROM user_system_data WHERE user_name = 'dave'",
                queryCaptor.getValue()
        );
    }

    @Test
    void getPassword_logsSqlExceptionAndReturnsDefault() throws Exception {
        // Arrange
        Logger logger = mock(Logger.class);
        // Inject mock logger (Lombok @Slf4j uses static field 'log')
        setLogger(logger);

        when(statement.executeQuery(anyString())).thenThrow(new SQLException("DB down"));

        // Act
        String password = lesson.getPassword();

        // Assert: falls back to default password "dave"
        assertEquals("dave", password);

        // Assert: error is logged via log.error
        verify(logger).error(
                eq("SQL Exception during password retrieval in getPassword method."),
                any(SQLException.class));
    }

    @Test
    void getPassword_logsGeneralExceptionAndReturnsDefault() throws Exception {
        // Arrange
        Logger logger = mock(Logger.class);
        setLogger(logger);

        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection failed"));

        // Act
        String password = lesson.getPassword();

        // Assert: still returns default
        assertEquals("dave", password);

        // Assert: error logged via log.error
        verify(logger).error(
                eq("General Exception during password retrieval in getPassword method."),
                any(RuntimeException.class));
    }

    /**
     * Utility to set the static SLF4J logger used by Lombok's @Slf4j.
     * This relies on reflection since Lombok generates a private static final field.
     * If this fails in the real environment, consider using a logging framework
     * test appender instead.
     */
    private void setLogger(Logger logger) throws Exception {
        java.lang.reflect.Field logField =
                SqlInjectionLesson6b.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(null, logger);
    }
}
