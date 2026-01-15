package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword no longer leaks stack traces via printStackTrace on SQLException")
    void getPassword_doesNotUsePrintStackTraceOnSQLException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Capture System.err output to verify generic error message instead of full stack trace
        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(errContent));

        try {
            // Act
            String password = lesson.getPassword();

            // Assert
            // 1) Method still returns a password value even when an exception occurs
            //    (behavior preserved  default "dave" is still returned)
            assertEquals("dave", password);

            String stderrOutput = errContent.toString();

            // 2) No stack trace-like content (e.g., "java.sql.SQLException") is printed
            //    This indirectly verifies that printStackTrace is no longer used.
            org.junit.jupiter.api.Assertions.assertFalse(
                    stderrOutput.contains("java.sql.SQLException"),
                    "stderr should not contain raw stack trace information");

            // 3) Generic error message is present (as implemented in the fix)
            org.junit.jupiter.api.Assertions.assertTrue(
                    stderrOutput.contains("An SQL error occurred during password retrieval."),
                    "stderr should contain a generic error message");
        } finally {
            // Restore System.err
            System.setErr(originalErr);
        }
    }

    @Test
    @DisplayName("getPassword handles non-SQL exceptions with generic message and default password")
    void getPassword_handlesGenericExceptionWithoutStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        // Force a generic exception from getConnection()
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Generic failure"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(errContent));

        try {
            // Act
            String password = lesson.getPassword();

            // Assert
            assertEquals("dave", password);

            String stderrOutput = errContent.toString();

            org.junit.jupiter.api.Assertions.assertFalse(
                    stderrOutput.contains("RuntimeException"),
                    "stderr should not leak raw stack trace details");

            org.junit.jupiter.api.Assertions.assertTrue(
                    stderrOutput.contains("An unexpected error occurred during password retrieval."),
                    "stderr should contain a generic error message for unexpected errors");
        } finally {
            System.setErr(originalErr);
        }
    }
}
