// Assuming standard Maven/Gradle test source layout and matching package.
// File path: src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on:
 * - Replacement of printStackTrace() with structured logging (no stack traces).
 * - Use of try-with-resources for Statement/ResultSet (invoked close()).
 */
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword should close Statement and ResultSet via try-with-resources")
    void getPassword_closesResources() throws Exception {
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
        when(resultSet.getString("password")).thenReturn("dbPassword");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        // Basic behavior remains
        assertEquals("dbPassword", password);

        // Because the production code uses try-with-resources for Statement and ResultSet,
        // close() must be called on both.
        verify(statement, times(1)).close();
        verify(resultSet, times(1)).close();
    }

    @Test
    @DisplayName("getPassword should log concise error instead of printing stack trace on SQLException")
    void getPassword_logsErrorWithoutPrintStackTraceOnSQLException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new java.sql.SQLException("DB failure"));

        java.sql.SQLException exception = new java.sql.SQLException("DB failure");

        java.sql.SQLException spySqle = Mockito.spy(exception);

        reset(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(spySqle);

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        // Method should return the default password and not rethrow.
        assertEquals("dave", password);

        // Critical delta assertion: printStackTrace must NOT be used anymore.
        verify(spySqle, never()).printStackTrace();
    }
}
