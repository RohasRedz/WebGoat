package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging / exception handling change
 * inside getPassword(): ensure non-null return and that exceptions are handled internally
 * (no stack traces thrown to caller).
 */
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword should return value from DB when query succeeds")
    void getPasswordReturnsValueFromDatabase() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("db-password", password, "Expected password read from DB when query succeeds");
    }

    @Test
    @DisplayName("getPassword should fall back to default and not throw when SQL exception occurs")
    void getPasswordHandlesSqlExceptionAndReturnsDefault() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("Simulated DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = null;
        Exception thrown = null;
        try {
            password = lesson.getPassword();
        } catch (Exception e) {
            thrown = e;
        }

        // Assert
        // Delta behavior: getPassword should log the error via log.error and swallow the exception,
        // returning the default "dave" instead of propagating or printing stack traces.
        assertNull(thrown, "getPassword should not propagate SQL exceptions to caller");
        assertNotNull(password, "Password should never be null even on exception");
        assertEquals("dave", password, "On failure, method should return the default password value");
    }

    @Test
    @DisplayName("getPassword should also handle generic exceptions from obtaining a connection")
    void getPasswordHandlesGenericExceptionAndReturnsDefault() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection pool down"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = null;
        Exception thrown = null;
        try {
            password = lesson.getPassword();
        } catch (Exception e) {
            thrown = e;
        }

        // Assert
        // This specifically exercises the outer catch (Exception e) block that now logs via log.error.
        assertNull(thrown, "getPassword should not propagate generic exceptions to caller");
        assertNotNull(password, "Password should never be null even on generic exception");
        assertEquals("dave", password, "On generic exception, method should still return default value");
    }
}
