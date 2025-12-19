package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests focused on secure logging and removal of direct stack trace printing
 * in SqlInjectionLesson6b.getPassword().
 *
 * These tests verify that:
 * - Exceptions during password retrieval no longer print stack traces to System.err.
 * - Password retrieval behavior (return value) is preserved.
 *
 * Note: We do not assert actual log output here; instead, we assert the absence
 * of printStackTrace() side effects by capturing System.err.
 */
class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    // Capture System.err to detect unintended printStackTrace usage.
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream errContent;

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
                anyInt()))
            .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dbPassword");

        errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
    }

    @Test
    void getPassword_returnsPasswordFromDatabaseWithoutPrintingStackTraceOnSuccess() {
        String password = lesson.getPassword();

        assertEquals("dbPassword", password);
        // On a normal successful path, there should be no stack trace output.
        String capturedErr = errContent.toString();
        org.junit.jupiter.api.Assertions.assertFalse(
                capturedErr.contains("Exception"),
                "No exception stack trace should be printed on successful execution");
    }

    @Test
    void getPassword_handlesSqlExceptionWithoutPrintingStackTrace() throws Exception {
        // Cause an SQLException from executeQuery
        reset(statement);
        when(connection.createStatement(anyInt(), anyInt())).thenReturn(statement);
        when(statement.executeQuery(anyString()))
                .thenThrow(new java.sql.SQLException("Database down"));

        String password = lesson.getPassword();

        // Should still return default value and not print stack trace
        assertEquals("dave", password, "Default password should be returned on failure");

        String capturedErr = errContent.toString();
        org.junit.jupiter.api.Assertions.assertFalse(
                capturedErr.contains("java.sql.SQLException"),
                "Stack trace for SQLException should not be printed to System.err");
    }

    @Test
    void getPassword_handlesGenericExceptionWithoutPrintingStackTrace() throws Exception {
        // Cause a generic exception from getConnection
        LessonDataSource brokenDataSource = mock(LessonDataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new RuntimeException("Connection factory failure"));

        SqlInjectionLesson6b brokenLesson = new SqlInjectionLesson6b(brokenDataSource);

        String password = brokenLesson.getPassword();

        assertEquals("dave", password, "Default password should be returned on generic failure");

        String capturedErr = errContent.toString();
        org.junit.jupiter.api.Assertions.assertFalse(
                capturedErr.contains("RuntimeException"),
                "Stack trace for generic Exception should not be printed to System.err");
    }
}
