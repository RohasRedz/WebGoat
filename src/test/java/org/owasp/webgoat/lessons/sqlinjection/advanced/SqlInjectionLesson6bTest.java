package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta unit tests for SqlInjectionLesson6b focusing on the changes:
 * - No hardcoded default password "dave".
 * - getPassword() returns DB value when present.
 * - getPassword() returns null when no row is returned or exceptions occur.
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
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
    }

    @Test
    void getPassword_returnsPasswordFromDatabaseWhenRowExists() throws Exception {
        // Arrange
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-secret");

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("db-secret", password);
        // Ensure the query for the fixed username is still executed
        verify(statement).executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'");
    }

    @Test
    void getPassword_returnsNullWhenNoRowExists() throws Exception {
        // Arrange
        when(resultSet.first()).thenReturn(false);

        // Act
        String password = lesson.getPassword();

        // Assert: default "dave" must not be returned anymore
        assertNull(password, "When no row exists, getPassword() should now return null, not a default value");
    }

    @Test
    void getPassword_returnsNullWhenSQLExceptionOccurs() throws Exception {
        // Arrange
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("DB error"));

        // Act
        String password = lesson.getPassword();

        // Assert: on error, we should not leak any default password
        assertNull(password, "On SQL exception, getPassword() should return null, not a hardcoded default");
    }

    @Test
    void getPassword_returnsNullWhenConnectionFails() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection error"));

        // Act
        String password = lesson.getPassword();

        // Assert
        assertNull(password, "On connection error, getPassword() should return null");
    }
}
