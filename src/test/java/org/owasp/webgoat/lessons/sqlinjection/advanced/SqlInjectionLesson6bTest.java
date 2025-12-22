package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("getPassword returns password from database on success and does not print stack trace")
    void getPassword_readsPasswordWithoutPrintingStackTrace() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dbPassword");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(errContent));

        try {
            String password = lesson.getPassword();

            assertEquals("dbPassword", password);

            String errOutput = errContent.toString();
            org.junit.jupiter.api.Assertions.assertFalse(
                    errOutput.contains("Exception") || errOutput.contains("at "),
                    "No stack trace or exception details should be printed");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    @DisplayName("getPassword handles SQL exception without printing stack trace and returns default password")
    void getPassword_handlesSqlException_withoutStackTraceExposure() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("DB is down"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(errContent));

        try {
            String password = lesson.getPassword();

            assertEquals("dave", password);

            String errOutput = errContent.toString();
            org.junit.jupiter.api.Assertions.assertFalse(
                    errOutput.contains("SQLException") || errOutput.contains("at "),
                    "No stack trace or SQL exception details should be printed");
        } finally {
            System.setErr(originalErr);
        }
    }
}
