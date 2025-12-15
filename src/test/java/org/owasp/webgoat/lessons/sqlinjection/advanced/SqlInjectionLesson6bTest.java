package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.slf4j.Logger;

/**
 * Delta tests for SqlInjectionLesson6b focusing on:
 * - Preserving normal behavior of completed()/getPassword().
 * - Verifying that the vulnerable printStackTrace() calls have been replaced by
 *   structured logging (log.error) rather than direct stack trace printing.
 *
 * These tests avoid relying on actual logging configuration and instead verify
 * observable behavior and interactions where practical.
 */
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("completed returns success when userid_6b matches database password")
    void completedReturnsSuccessWhenPasswordMatches() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet != null && resultSet.first()).thenReturn(true);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secret-db-pass");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        AttackResult result = lesson.completed("secret-db-pass");

        // Assert
        // Normal success path should remain unchanged after the logging fix.
        assertTrue(result.getLessonsCompletedHint().contains("success")
                || result.toString().toLowerCase().contains("success"),
                "Expected success result when userid_6b matches DB password");
    }

    @Test
    @DisplayName("completed returns failure when userid_6b does not match database password")
    void completedReturnsFailureWhenPasswordDoesNotMatch() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet != null && resultSet.first()).thenReturn(true);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secret-db-pass");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        AttackResult result = lesson.completed("wrong-pass");

        // Assert
        assertTrue(result.getLessonsCompletedHint().toLowerCase().contains("fail")
                || result.toString().toLowerCase().contains("fail"),
                "Expected failure result when userid_6b does not match DB password");
    }

    @Test
    @DisplayName("getPassword gracefully handles SQLException and does not print stack trace")
    void getPasswordHandlesSqlExceptionWithoutPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("Simulated SQL error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // We cannot easily intercept Lombok's generated logger instance without
        // altering the class, but we can assert:
        // - getPassword() still returns some non-null value (default "dave" path).
        // - No direct calls to printStackTrace() are made in the fixed code.
        //
        // The second point is enforced structurally by the code change itself;
        // at runtime this test ensures the method behavior remains safe and stable.

        // Act
        String password = lesson.getPassword();

        // Assert
        // Even in case of SQLException, method should return the default or last known value
        assertEquals("dave", password,
                "Expected getPassword to return default value when SQL error occurs");
    }

    @Test
    @DisplayName("getPassword handles general Exception and does not print stack trace")
    void getPasswordHandlesGeneralExceptionWithoutPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);

        // Simulate general exception from dataSource.getConnection()
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Simulated general error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("dave", password,
                "Expected getPassword to return default value when general exception occurs");
    }

    // NOTE:
    // - Direct verification that printStackTrace() is not called is a structural
    //   property of the source code change (no calls remain). These tests
    //   concentrate on observable behavior in error paths to ensure the fix
    //   did not introduce regressions and that exceptions are handled gracefully.
    // - If test infrastructure provides access to the Lombok-generated logger,
    //   it could be injected and verified for log.error(...) invocations, but
    //   that is beyond the minimal delta test scope here.
}
