package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionLesson6b focusing on:
 * - completed() success/failure logic around getPassword()
 * - ensuring getPassword() does not leak stack traces when exceptions occur.
 */
class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = Mockito.mock(LessonDataSource.class);
        lesson = new SqlInjectionLesson6b(dataSource);

        connection = Mockito.mock(Connection.class);
        statement = Mockito.mock(Statement.class);
        resultSet = Mockito.mock(ResultSet.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(
                        connection.createStatement(
                                Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(statement);
        Mockito.when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
    }

    @Test
    void completed_returnsSuccessWhenUseridMatchesPassword() throws IOException, Exception {
        // Arrange: getPassword returns "secret"
        Mockito.when(resultSet.first()).thenReturn(true);
        Mockito.when(resultSet.getString("password")).thenReturn("secret");

        // Act
        AttackResult result = lesson.completed("secret");

        // Assert
        assertEquals(
                true,
                result.getLessonCompleted(),
                "Expected success when userid_6b equals retrieved password");
    }

    @Test
    void completed_returnsFailureWhenUseridDoesNotMatchPassword() throws IOException, Exception {
        // Arrange: database returns some password
        Mockito.when(resultSet.first()).thenReturn(true);
        Mockito.when(resultSet.getString("password")).thenReturn("secret");

        // Act
        AttackResult result = lesson.completed("not-secret");

        // Assert
        assertEquals(
                false,
                result.getLessonCompleted(),
                "Expected failure when userid_6b does not equal retrieved password");
    }

    @Test
    void getPassword_doesNotPrintStackTraceOnSqlException() throws Exception {
        // Arrange: force an SQLException from executeQuery
        Mockito.when(statement.executeQuery(Mockito.anyString()))
                .thenThrow(new java.sql.SQLException("DB error"));

        // Capture System.err to ensure no stack trace is printed
        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(errContent));

        try {
            String password = lesson.getPassword();
            // Even on error, method returns default "dave"
            assertEquals("dave", password, "Expected default password when query fails");

            String logged = errContent.toString();
            // Assert: no stack trace text for the SQLException (the fix removed printStackTrace)
            org.junit.jupiter.api.Assertions.assertFalse(
                    logged.contains("java.sql.SQLException"),
                    "Expected no SQLException stack trace in stderr");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void getPassword_doesNotPrintStackTraceOnGenericException() throws Exception {
        // Arrange: make dataSource.getConnection() throw a generic exception
        LessonDataSource failingDataSource = Mockito.mock(LessonDataSource.class);
        Mockito.when(failingDataSource.getConnection())
                .thenThrow(new RuntimeException("Connection failure"));
        SqlInjectionLesson6b failingLesson = new SqlInjectionLesson6b(failingDataSource);

        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(errContent));

        try {
            String password = failingLesson.getPassword();
            assertEquals("dave", password, "Expected default password when connection fails");

            String logged = errContent.toString();
            org.junit.jupiter.api.Assertions.assertFalse(
                    logged.contains("RuntimeException")
                            || logged.contains("java.lang.RuntimeException"),
                    "Expected no generic exception stack trace in stderr");
        } finally {
            System.setErr(originalErr);
        }
    }
}
