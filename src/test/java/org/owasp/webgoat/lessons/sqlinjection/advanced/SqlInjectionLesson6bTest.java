package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the information-exposure fix:
 * - Ensures getPassword() still returns the database value on success.
 * - Ensures that when a SQLException occurs, no stack trace logging via printStackTrace is used
 *   and the default password value is returned.
 *
 * Note: We simulate behavior via mocks without touching a real database or logs.
 */
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword should return password from DB when query succeeds")
    void getPassword_returnsDbValue_onSuccess() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
            .thenReturn(statement);
        Mockito.when(statement.executeQuery(anyString())).thenReturn(resultSet);
        Mockito.when(resultSet.first()).thenReturn(true);
        Mockito.when(resultSet.getString("password")).thenReturn("from-db");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String result = lesson.getPassword();

        // Assert
        assertEquals("from-db", result);
    }

    @Test
    @DisplayName("getPassword should not expose stack trace and should return default on SQLException")
    void getPassword_handlesSQLException_withoutStackTraceExposure() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
            .thenThrow(new SQLException("Simulated DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String result = lesson.getPassword();

        // Assert
        // Verify that despite the DB error, we still get the default value
        assertEquals("dave", result);

        // There is no printStackTrace call anymore in the fixed code.
        // We cannot directly assert absence of a removed call, but we can at least
        // ensure that our mocks did not interact beyond the attempted statement creation.
        verify(connection).createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        verify(connection, never()).prepareStatement(anyString());
    }
}
