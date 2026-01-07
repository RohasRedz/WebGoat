// Assuming package based on source file location.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the change that removed
 * printStackTrace() calls in the catch blocks to prevent information leakage.
 *
 * Since this is a delta test, we focus on:
 * - Behavior in the normal (no-exception) path is preserved.
 * - When exceptions occur, the method still returns a value but no stack traces are printed
 *   (indirectly validated by not expecting exceptions and by simulating error paths).
 *
 * NOTE: Directly asserting on logging output is avoided; instead we ensure that
 * exceptions thrown inside data-access logic do not escape and that the method
 * still returns a non-null password even when the data source misbehaves.
 */
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword should return value from database when query succeeds (behavior preserved)")
    void getPasswordReturnsValueFromDatabase() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("db-password", password,
                "Expected getPassword to return value retrieved from the database");
    }

    @Test
    @DisplayName("getPassword should not throw when SQLException occurs and should fall back to default (no stack trace leak)")
    void getPasswordHandlesSqlExceptionWithoutThrowing() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        // Simulate SQLException in createStatement
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        // The method should swallow the exception and return the default "dave"
        // (instead of propagating or crashing, and without printing stack traces).
        assertEquals("dave", password,
                "Expected getPassword to return default when SQLException occurs");
    }

    @Test
    @DisplayName("getPassword should handle generic Exception from getConnection gracefully")
    void getPasswordHandlesGenericExceptionWithoutThrowing() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        // Simulate a generic exception from getConnection
        when(dataSource.getConnection()).thenThrow(new RuntimeException("connection failure"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        // With the fix, generic exceptions are also swallowed in the outer try-catch,
        // and the default value is returned.
        assertEquals("dave", password,
                "Expected getPassword to return default when getConnection throws an exception");
    }
}
