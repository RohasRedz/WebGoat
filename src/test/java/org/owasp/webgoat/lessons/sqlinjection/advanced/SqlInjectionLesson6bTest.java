package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta unit tests for SqlInjectionLesson6b:
 * - Ensure behavioral equivalence for getPassword() and completed() after logging changes.
 * - Indirectly exercise paths where exceptions are logged instead of printStackTrace.
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
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
    }

    @Test
    void getPassword_returnsDatabasePasswordWhenAvailable() throws Exception {
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dbPassword");

        String password = lesson.getPassword();

        assertEquals("dbPassword", password,
                "getPassword should still return the value from DB after logging fix");
    }

    @Test
    void getPassword_returnsDefaultWhenExceptionOccursButDoesNotThrow() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection failure"));

        String password = lesson.getPassword();

        // Behavior before and after: should fall back to default "dave" and not throw
        assertEquals("dave", password,
                "When an exception occurs, getPassword should still return default and not propagate exceptions");
    }

    @Test
    void completed_returnsSuccessWhenUserIdMatchesPassword() throws IOException {
        // Simulate DB returning "secret" as password, and user supplying same value
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secret");

        AttackResult result = lesson.completed("secret");

        assertTrue(result.isCorrect(),
                "completed() must still succeed when supplied userid_6b equals actual password");
    }

    @Test
    void completed_returnsFailureWhenUserIdDoesNotMatchPassword() throws IOException {
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secret");

        AttackResult result = lesson.completed("wrong");

        assertTrue(result.isError() || !result.isCorrect(),
                "completed() must still fail when userid_6b does not equal the password");
    }

    @Test
    void getPassword_logsSqlExceptionWithoutThrowing() throws Exception {
        // Force an SQLException path in inner try/catch
        when(statement.executeQuery(anyString())).thenThrow(new java.sql.SQLException("SQL error"));

        String password = lesson.getPassword();

        // Still returns default or last value, and does not propagate the exception
        assertEquals("dave", password,
                "On SQL exception, getPassword should log and return default password");
    }
}
