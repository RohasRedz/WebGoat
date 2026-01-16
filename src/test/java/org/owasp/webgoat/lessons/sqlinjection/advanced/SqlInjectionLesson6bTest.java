// File path: src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the changed logging behavior:
 * - printStackTrace() was replaced with Slf4j log.error() to avoid direct stack trace exposure.
 * These tests assert that:
 * - getPassword() still returns the fallback password when an exception occurs.
 * - stack traces are not printed to System.err by printStackTrace() anymore.
 */
@Slf4j
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword returns fallback when SQLException occurs and does not print stack trace")
    void getPassword_usesFallbackAndAvoidsPrintStackTraceOnSQLException() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Connection connection = Mockito.mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Capture System.err to ensure no stack trace is printed.
        PrintStream originalErr = System.err;
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        try {
            // Act
            String password = lesson.getPassword();

            // Assert
            // Fallback value still returned
            assertEquals("dave", password);
            // The previous implementation used printStackTrace(); after the fix we expect
            // no direct stack trace output to System.err from this method.
            String errOutput = errContent.toString();
            // It is enough to assert that our specific error message is not printed as a stack trace.
            // We avoid asserting on logger internals; only on lack of printStackTrace() behavior.
            org.junit.jupiter.api.Assertions.assertFalse(
                    errOutput.contains("java.sql.SQLException: DB error"),
                    "SQLException stack trace should not be printed to System.err");
        } finally {
            System.setErr(originalErr);
        }
    }
}
