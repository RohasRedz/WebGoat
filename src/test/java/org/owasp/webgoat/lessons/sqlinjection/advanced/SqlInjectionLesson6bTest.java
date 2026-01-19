package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword returns DB password when query succeeds (behavior preserved)")
    void getPassword_returnsDatabaseValueOnSuccess() throws Exception {
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
        when(resultSet.getString("password")).thenReturn("db-secret");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("db-secret", password, "Password should be taken from DB when query succeeds");
    }

    @Test
    @DisplayName("getPassword no longer returns hardcoded default when query fails")
    void getPassword_doesNotExposeHardcodedDefaultOnFailure() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        // Simulate query that returns no rows
        when(resultSet.first()).thenReturn(false);

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("", password,
                "When no DB row is found, password should now be empty instead of a hardcoded default");
    }

    @Test
    @DisplayName("getPassword logs error instead of printing stack trace on SQLException")
    void getPassword_logsErrorOnSqlException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new java.sql.SQLException("DB down"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        // We cannot easily assert log calls without a logging test appender,
        // but we can assert that the method falls back to empty string and does not throw.
        assertEquals("", password, "On SQLException, getPassword should safely return empty string");
    }
}
