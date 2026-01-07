// Delta test for BATCH-002: SqlInjectionLesson6b.java
// File path (inferred): src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java

package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

// Delta focus for this class:
// - Removal of printStackTrace() to avoid information exposure through log files
// - Behavior of completed() (success/failure) must remain unchanged.

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("completed still succeeds when provided userid equals DB password")
    void completed_behaviourUnchanged_onCorrectPassword() throws Exception {
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
        when(resultSet.getString("password")).thenReturn("secretFromDb");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        AttackResult result = lesson.completed("secretFromDb");

        // Assert: behavior is unchanged (success when provided password matches DB password)
        assertTrue(result.getLessonCompleted(), "completed should still succeed when passwords match");
    }

    @Test
    @DisplayName("getPassword does not print stack traces on SQLException or generic Exception")
    void getPassword_doesNotPrintStackTraces_onExceptions() throws Exception {
        // Arrange: dataSource.getConnection throws to simulate failure path
        LessonDataSource dataSource = mock(LessonDataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB down"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Capture System.err to ensure no stack trace is printed
        PrintStream originalErr = System.err;
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        try {
            // Act
            String password = lesson.getPassword();

            // Assert: method still returns a value but does not print stack trace content
            assertFalse(errContent.toString().contains("RuntimeException"),
                    "Stack trace or exception details should not be printed to stderr");
            // Password may remain default; we only care that no printing occurs
            // (no assertion on the value itself).
        } finally {
            System.setErr(originalErr);
        }
    }
}
