package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.LoggerFactory;

/**
 * Delta unit tests for SqlInjectionLesson6b focusing only on changed behavior:
 * - Replacing printStackTrace() with structured logging via SLF4J.
 * These tests do not attempt to validate full logging content, only that
 * exceptions are handled without throwing and that core behavior is preserved.
 */
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword logs SQL exceptions via SLF4J and returns default without throwing")
    void getPassword_logsSqlException_andReturnsDefault() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new java.sql.SQLException("boom"));

        // Capture log level to ensure no runtime issues (basic sanity for logging path)
        Logger logger = (Logger) LoggerFactory.getLogger(SqlInjectionLesson6b.class);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.ERROR);

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert  on SQL exception, method should not throw and should return default "dave"
        assertEquals("dave", password, "On SQL exception, default password should be returned and error logged");

        // Cleanup logger level
        logger.setLevel(originalLevel);
    }

    @Test
    @DisplayName("getPassword still retrieves password when query succeeds (behavior unchanged)")
    void getPassword_returnsPasswordFromDatabase_onSuccess() throws Exception {
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
        when(resultSet.getString("password")).thenReturn("dbPassword");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert  functional behavior preserved
        assertEquals("dbPassword", password, "Password should still be read from the database when query succeeds");
    }
}
