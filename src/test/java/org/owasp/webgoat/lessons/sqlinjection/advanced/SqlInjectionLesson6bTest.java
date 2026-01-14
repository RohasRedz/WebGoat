package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging / information exposure fix.
 *
 * Since the fix removes direct stack trace printing and avoids leaking internal details,
 * these tests:
 *  - Ensure that exceptions thrown during getPassword() do not propagate sensitive data
 *    through explicit stack trace calls.
 *  - Verify that functional behavior (success/fail based on password match) is preserved.
 */
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("completed returns success when provided userid matches DB password (behavior preserved)")
    void completed_returnsSuccessWhenPasswordMatches() throws IOException, SQLException {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                Mockito.anyInt(),
                Mockito.anyInt()))
                .thenReturn(statement);
        when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dave");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        AttackResult result = lesson.completed("dave");

        // Assert
        org.junit.jupiter.api.Assertions.assertTrue(result.getLessonCompleted());
    }

    @Test
    @DisplayName("completed returns failure when userid does not match password (behavior preserved)")
    void completed_returnsFailureWhenPasswordDoesNotMatch() throws IOException, SQLException {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                Mockito.anyInt(),
                Mockito.anyInt()))
                .thenReturn(statement);
        when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dave");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        AttackResult result = lesson.completed("not-dave");

        // Assert
        org.junit.jupiter.api.Assertions.assertFalse(result.getLessonCompleted());
    }

    @Test
    @DisplayName("getPassword handles SQL exception without propagating it (no stack trace exposure at API level)")
    void getPassword_handlesSQLExceptionWithoutPropagation() throws SQLException {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Connection connection = Mockito.mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                Mockito.anyInt(),
                Mockito.anyInt()))
                .thenThrow(new SQLException("simulated SQL error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        // The method should swallow the exception and return the default value "dave",
        // and it must not propagate the SQLException.
        org.junit.jupiter.api.Assertions.assertEquals("dave", password);

        // We cannot directly assert the absence of printStackTrace calls via Mockito.
        // Instead, we assert that no exception escapes getPassword, ensuring callers
        // are not exposed to internal errors.
        verify(connection).createStatement(
                Mockito.anyInt(),
                Mockito.anyInt());
    }

    @Test
    @DisplayName("completed does not throw even when getPassword encounters SQL exception")
    void completed_doesNotThrowWhenGetPasswordFails() throws SQLException, IOException {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Connection connection = Mockito.mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                Mockito.anyInt(),
                Mockito.anyInt()))
                .thenThrow(new SQLException("simulated SQL error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        AttackResult result = lesson.completed("any");

        // Assert
        org.junit.jupiter.api.Assertions.assertFalse(result.getLessonCompleted());
        // Completed should handle the internal failure gracefully.
        verify(connection).createStatement(Mockito.anyInt(), Mockito.anyInt());
        verify(connection, never()).prepareStatement(Mockito.anyString());
    }
}
