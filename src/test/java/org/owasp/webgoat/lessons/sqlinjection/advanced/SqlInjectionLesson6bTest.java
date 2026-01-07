package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword returns DB password when query succeeds (baseline behavior preserved)")
    void getPassword_returnsPasswordOnSuccess() throws Exception {
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
        when(resultSet.getString("password")).thenReturn("secret-from-db");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertThat(password).isEqualTo("secret-from-db");
    }

    @Test
    @DisplayName("getPassword logs error via Slf4j and does not call printStackTrace on SQLException")
    void getPassword_doesNotUsePrintStackTraceOnSQLException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString()))
                .thenThrow(new SQLException("Simulated SQL error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Capture System.err to assert that stack traces are not dumped there anymore.
        // This is a proxy for "no printStackTrace()" calls.
        java.io.PrintStream originalErr = System.err;
        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        System.setErr(new java.io.PrintStream(errContent));

        try {
            // Act
            String password = lesson.getPassword();

            // Assert
            // On error, getPassword should fall back to the default "dave"
            assertThat(password).isEqualTo("dave");

            // Ensure that nothing resembling a stack trace got written to System.err.
            String errOutput = errContent.toString();
            assertThat(errOutput)
                    .as("System.err should not contain stack trace from printStackTrace()")
                    .doesNotContain("java.sql.SQLException")
                    .doesNotContain("at org.owasp.webgoat");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    @DisplayName("getPassword logs generic error and still returns default password when connection acquisition fails")
    void getPassword_handlesGenericExceptionWithoutStackTraceLeak() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        java.io.PrintStream originalErr = System.err;
        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        System.setErr(new java.io.PrintStream(errContent));

        try {
            // Act
            String password = lesson.getPassword();

            // Assert
            assertThat(password).isEqualTo("dave");
            String errOutput = errContent.toString();
            assertThat(errOutput)
                    .as("System.err should not contain raw stack trace")
                    .doesNotContain("java.lang.RuntimeException")
                    .doesNotContain("at org.owasp.webgoat");
        } finally {
            System.setErr(originalErr);
        }
    }
}
