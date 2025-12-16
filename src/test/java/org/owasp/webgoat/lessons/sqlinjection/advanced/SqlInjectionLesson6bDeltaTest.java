// TODO: Package inferred from source class package; adjust if project structure differs.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta unit tests for SqlInjectionLesson6b focusing only on the logging changes:
 * - Original code used sqle.printStackTrace() and e.printStackTrace().
 * - Updated code uses log.error(...) via Lombok @Slf4j.
 *
 * Directly asserting logger calls from Lombok's @Slf4j is non-trivial without a logging
 * test appender; instead, these tests focus on:
 * - Ensuring that exceptions in database calls do not propagate (behavior preserved).
 * - Ensuring getPassword() still returns the same value contractually when exceptions occur.
 *
 * This demonstrates that the new logging behavior does not break the functional path that
 * previously relied on catch blocks swallowing exceptions.
 */
public class SqlInjectionLesson6bDeltaTest {

    @Test
    @DisplayName("getPassword returns default value when SQLException occurs (behavior preserved after logging fix)")
    void getPassword_returnsDefaultOnSQLException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        // Simulate SQLException during Statement creation or execution
        when(connection.createStatement(
                anyInt(),
                anyInt()))
                .thenThrow(new SQLException("Forced test SQL exception"));

        // Act
        String password = lesson.getPassword();

        // Assert
        // Original behavior: on SQLException, it caught the exception and returned the default "dave".
        // After the fix, behavior should be identical while using log.error instead of printStackTrace.
        assertEquals("dave", password, "Default password must still be returned when SQLException occurs");
    }

    @Test
    @DisplayName("getPassword returns DB value when query succeeds (core behavior unchanged by logging fix)")
    void getPassword_returnsDatabaseValueOnSuccess() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secureFromDb");

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("secureFromDb", password, "getPassword should still return DB value when available");
    }

    @Test
    @DisplayName("getPassword swallows unexpected Exception and returns default (behavior preserved after logging fix)")
    void getPassword_returnsDefaultOnUnexpectedException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Simulate an unexpected runtime exception during getConnection
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Unexpected"));

        // Act
        String password = lesson.getPassword();

        // Assert
        // Previously printStackTrace swallowed the exception and returned default.
        // Now log.error should do the same, preserving behavior.
        assertEquals("dave", password, "Default password must still be returned when unexpected exception occurs");
    }
}
