// TODO: Package name inferred from the updated source; adjust if the real package differs.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta unit tests for SqlInjectionLesson6b.
 *
 * Vulnerability description:
 * - Exceptions were previously handled with printStackTrace(), risking information exposure in logs.
 *
 * Changed behavior verified here:
 * - Exceptions in getPassword() are now handled via SLF4J log.error(...) instead of printStackTrace().
 * - Method semantics (default password vs DB password) are preserved.
 */
public class SqlInjectionLesson6bTest {

    private LessonDataSource mockLessonDataSource(Connection connection) throws Exception {
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        when(lessonDataSource.getConnection()).thenReturn(connection);
        return lessonDataSource;
    }

    @Test
    @DisplayName("getPassword swallows SQLExceptions and returns default value (logging instead of printStackTrace)")
    void getPassword_onSqlException_returnsDefault_andDoesNotThrow() throws Exception {
        // Arrange: simulate a SQL error during statement creation
        Connection connection = mock(Connection.class);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("Simulated SQL error"));

        LessonDataSource lessonDataSource = mockLessonDataSource(connection);
        SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(lessonDataSource);

        // Act & Assert: no exception is propagated and default value is returned
        String password = assertDoesNotThrow(endpoint::getPassword);
        assertEquals("dave", password, "On SQL error, getPassword() should fall back to default value");
    }

    @Test
    @DisplayName("getPassword still returns DB value when query succeeds (behavior preserved after logging change)")
    void getPassword_onSuccess_returnsDatabaseValue() throws Exception {
        // Arrange: simulate successful query
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        LessonDataSource lessonDataSource = mockLessonDataSource(connection);
        SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(lessonDataSource);

        // Act
        String password = endpoint.getPassword();

        // Assert: original semantics preserved
        assertEquals("db-password", password);
    }
}
