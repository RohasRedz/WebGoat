package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on:
 * - removal of hard-coded default password
 * - use of PreparedStatement instead of hard-coded SQL
 * - avoiding raw stack trace exposure via printStackTrace
 *
 * Target file (after fix):
 * src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6b.java
 */
@Slf4j
public class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson6b;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        lesson6b = new SqlInjectionLesson6b(dataSource);

        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void getPassword_readsFromDatabaseWithoutHardcodedFallback() throws Exception {
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        String password = lesson6b.getPassword();

        // Verify parameterized query is used for fixed username "dave"
        verify(connection).prepareStatement("SELECT password FROM user_system_data WHERE user_name = ?");
        verify(preparedStatement).setString(1, "dave");

        // The returned password must match DB content, not a constant
        assertEquals("db-password", password);
    }

    @Test
    void getPassword_returnsNullWhenNoRowFoundAndNoFallbackConstant() throws Exception {
        when(resultSet.first()).thenReturn(false);

        String password = lesson6b.getPassword();

        // When no data is found, the original hard-coded "dave" fallback must not be used
        assertEquals(null, password);
    }

    @Test
    void getPassword_handlesSqlExceptionWithoutPrintStackTrace() throws Exception {
        // Simulate an SQLException when creating or executing the statement
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        // Capture any stack trace attempts by replacing System.err temporarily
        PrintWriter backupErr = new PrintWriter(System.err);
        StringWriter sw = new StringWriter();
        System.setErr(new PrintWriter(sw));

        try {
            String password = lesson6b.getPassword();

            // In case of exception, method should still return null (no fallback)
            assertEquals(null, password);

            // Ensure no raw stack trace from printStackTrace is emitted
            String errOutput = sw.toString();
            org.junit.jupiter.api.Assertions.assertFalse(
                    errOutput.contains("java.sql.SQLException"),
                    "printStackTrace should not be used for SQLExceptions anymore");
        } finally {
            // Restore System.err
            System.setErr(backupErr);
        }
    }
}
