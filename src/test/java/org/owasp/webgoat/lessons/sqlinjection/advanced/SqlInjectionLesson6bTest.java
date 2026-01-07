package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on the behavior
 * changed by the fix:
 * - getPassword no longer returns a hard-coded "dave" on error/empty result.
 * - Exceptions are logged and not rethrown as stack traces.
 */
class SqlInjectionLesson6bTest {

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
                anyInt())).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
    }

    @Test
    void getPassword_whenNoRowInDatabase_doesNotReturnHardCodedDave() throws Exception {
        // Arrange: simulate empty result set (no rows)
        when(resultSet.first()).thenReturn(false);

        // Act
        String password = lesson.getPassword();

        // Assert: default password is now empty string and not the old hard-coded "dave"
        assertNotNull(password, "Password should never be null");
        assertEquals("", password, "Password should be empty string when no DB row is found");
        assertNotEquals("dave", password, "Password must not fall back to hard-coded 'dave' anymore");
    }

    @Test
    void getPassword_whenSQLExceptionOccurs_returnsEmptyStringAndDoesNotThrow() throws Exception {
        // Arrange: simulate SQL exception during statement creation or query execution
        when(connection.createStatement(anyInt(), anyInt())).thenThrow(new SQLException("DB failure"));

        // Act & Assert: method should handle the exception, log it, and return the default (empty) password
        String password = assertDoesNotThrow(lesson::getPassword,
                "getPassword should not propagate SQLExceptions");

        assertEquals("", password, "On SQL error, password should default to empty string (not 'dave')");
    }

    @Test
    void completed_returnsSuccessOnlyWhenSuppliedUseridMatchesDatabasePassword() throws Exception {
        // Arrange: simulate DB returning a specific password for user 'dave'
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secret-db-password");

        // First call: correct password -> success
        AttackResult successResult = lesson.completed("secret-db-password");
        assertTrue(successResult.getLessonCompleted(),
                "completed should succeed when supplied userid_6b equals DB password");

        // Second call: incorrect password -> failure
        AttackResult failureResult = lesson.completed("wrong-password");
        assertFalse(failureResult.getLessonCompleted(),
                "completed should fail when supplied userid_6b does not equal DB password");
    }

    @Test
    void getPassword_whenGeneralExceptionOccurs_returnsEmptyStringAndDoesNotThrow() throws Exception {
        // Arrange: simulate an unchecked/other exception from dataSource.getConnection()
        when(dataSource.getConnection()).thenThrow(new RuntimeException("unexpected"));

        // Act & Assert
        String password = assertDoesNotThrow(lesson::getPassword,
                "getPassword should catch and log unexpected exceptions");

        assertEquals("", password, "On general exception, password should default to empty string");
    }
}
