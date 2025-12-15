package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on the changed behavior:
 * - Ensuring business logic of completed()/getPassword() is preserved.
 * - Ensuring exceptions in getPassword() are handled and do not propagate.
 *
 * NOTE: Direct verification of SLF4J logger calls is difficult because the logger
 * is a private static final field. Instead, we verify that:
 *   - Exceptions thrown by JDBC calls no longer propagate to the caller.
 *   - The method still returns a value and completed() behaves correctly,
 * which is the observable behavior change related to secure logging.
 */
class SqlInjectionLesson6bTest {

    /**
     * Helper subclass to expose getPassword() for controlled testing without
     * touching the logger implementation details.
     */
    private static class TestableSqlInjectionLesson6b extends SqlInjectionLesson6b {
        TestableSqlInjectionLesson6b(LessonDataSource dataSource) {
            super(dataSource);
        }

        @Override
        protected String getPassword() {
            return super.getPassword();
        }
    }

    @Test
    @DisplayName("completed returns success when userid_6b equals getPassword() result")
    void completedReturnsSuccessWhenUserIdMatchesPassword() throws IOException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        TestableSqlInjectionLesson6b endpoint = spy(new TestableSqlInjectionLesson6b(dataSource));

        // Force getPassword() to return a known value
        doReturn("secret-pass").when(endpoint).getPassword();

        // Act
        AttackResult result = endpoint.completed("secret-pass");

        // Assert
        assertNotNull(result, "AttackResult must not be null");
        assertTrue(result.isSuccessful(), "Result should be successful when userid_6b equals password");
    }

    @Test
    @DisplayName("completed returns failed when userid_6b does not equal getPassword() result")
    void completedReturnsFailedWhenUserIdDoesNotMatchPassword() throws IOException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        TestableSqlInjectionLesson6b endpoint = spy(new TestableSqlInjectionLesson6b(dataSource));

        // Force getPassword() to return a known value
        doReturn("secret-pass").when(endpoint).getPassword();

        // Act
        AttackResult result = endpoint.completed("wrong-pass");

        // Assert
        assertNotNull(result, "AttackResult must not be null");
        assertFalse(result.isSuccessful(), "Result should be failed when userid_6b does not equal password");
    }

    @Test
    @DisplayName("getPassword handles SQL exception without propagating and still returns a value")
    void getPasswordHandlesSqlExceptionWithoutPropagation() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY))
            .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("DB error"));

        SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(dataSource);

        // Act
        // If the logging fix is correct, SQLException should be caught and not propagated,
        // and getPassword() should still return a (fallback) password.
        String password = endpoint.getPassword();

        // Assert
        assertNotNull(password, "Password should not be null even when SQL exception occurs");
        assertFalse(password.isEmpty(), "Password should not be empty even when SQL exception occurs");
        // In the current implementation, the fallback is the initial literal "dave".
        assertEquals("dave", password, "Fallback password should remain the default value when an exception occurs");

        // Also ensure the JDBC interactions happened as expected
        InOrder inOrder = inOrder(dataSource, connection, statement);
        inOrder.verify(dataSource).getConnection();
        inOrder.verify(connection).createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        inOrder.verify(statement).executeQuery(anyString());
    }

    @Test
    @DisplayName("getPassword handles generic exception without propagating and still returns a value")
    void getPasswordHandlesGenericExceptionWithoutPropagation() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);

        // Cause dataSource.getConnection() itself to throw a RuntimeException
        when(dataSource.getConnection()).thenThrow(new RuntimeException("connection failure"));

        SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(dataSource);

        // Act
        // If the logging fix is correct, generic exceptions should be caught at the outer catch
        // and not propagate to the caller.
        String password = endpoint.getPassword();

        // Assert
        assertNotNull(password, "Password should not be null even when a generic exception occurs");
        assertFalse(password.isEmpty(), "Password should not be empty even when a generic exception occurs");
        assertEquals("dave", password, "Fallback password should remain the default value when an exception occurs");
    }

    @Test
    @DisplayName("Logger is resolvable and class is loggable (sanity check on SLF4J integration)")
    void loggerIsResolvableForClass() {
        // This test does not verify logging calls directly (since logger is private static final),
        // but it ensures that SLF4J configuration for this class is syntactically valid and
        // does not cause any linkage problems.
        Logger logger = LoggerFactory.getLogger(SqlInjectionLesson6b.class);
        assertNotNull(logger, "Logger should be obtainable for SqlInjectionLesson6b");
    }
}
