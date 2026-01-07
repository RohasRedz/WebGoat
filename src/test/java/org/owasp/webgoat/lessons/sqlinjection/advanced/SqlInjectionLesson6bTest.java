/*
 * Delta test for BATCH-002 - SqlInjectionLesson6b.java
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

import static org.mockito.Mockito.*;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the removal of stack trace logging
 * and avoiding information exposure through logs.
 *
 * Jira: SVCF-1810
 */
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword() should not print stack traces and should log generic error message on SQLException")
    void getPassword_usesGenericErrorLoggingOnSqlException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("Simulated failure"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Capture System.err to verify that no full stack trace is printed
        PrintStream originalErr = System.err;
        ByteArrayOutputStream errOut = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errOut));

        try {
            // Act
            String password = lesson.getPassword();

            // Assert
            // Ensure generic message is printed instead of stack trace
            String logged = errOut.toString();
            org.junit.jupiter.api.Assertions.assertTrue(
                    logged.contains("An SQL error occurred during password retrieval."),
                    "Expected a generic SQL error message in logs");

            // Stack traces usually contain 'java.sql.SQLException' and 'at ' lines;
            // we assert they are not present to avoid information exposure.
            org.junit.jupiter.api.Assertions.assertFalse(
                    logged.contains("java.sql.SQLException"),
                    "Stack trace details must not be logged");
            org.junit.jupiter.api.Assertions.assertFalse(
                    logged.contains("at "),
                    "Stack trace frames must not be logged");

            // Behavior should still return some password value (default value preserved)
            assertEquals("dave", password);
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    @DisplayName("getPassword() should return database value when query succeeds (unchanged behavior guard)")
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
        assertEquals("db-secret", password);
    }
}
