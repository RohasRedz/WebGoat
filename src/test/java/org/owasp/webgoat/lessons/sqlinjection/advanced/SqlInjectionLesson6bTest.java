package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests focusing on the information exposure fix:
 * - Ensures completed() delegates to the new checkPassword() boolean method.
 * - Verifies that checkPassword() does not expose the retrieved password
 *   and only returns a boolean based on comparison.
 */
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("completed() should return success only when checkPassword() returns true")
    void completedUsesCheckPasswordBoolean() throws IOException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = spy(new SqlInjectionLesson6b(dataSource));

        String providedPassword = "secret";

        // Stub the internal checkPassword to ensure completed() behavior depends on it
        doReturn(true).when(lesson).checkPassword(providedPassword);

        // Act
        AttackResult result = lesson.completed(providedPassword);

        // Assert
        assertNotNull(result, "AttackResult should not be null");
        assertTrue(result.getLessonCompleted(), "Lesson should be marked as completed when checkPassword() returns true");
        verify(lesson).checkPassword(providedPassword);
    }

    @Test
    @DisplayName("checkPassword() should compare provided password with stored one without exposing it")
    void checkPasswordPerformsInternalComparisonOnly() throws Exception {
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
        boolean match = lesson.checkPassword("dbPassword");
        boolean noMatch = lesson.checkPassword("wrongPassword");

        // Assert
        assertTrue(match, "checkPassword() should return true when provided password matches stored password");
        assertFalse(noMatch, "checkPassword() should return false when provided password does not match stored password");

        // Verify that a fixed query is used and that only boolean is returned
        verify(statement).executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'");
        // Critically, there is no way to retrieve the password from checkPassword():
        // the method returns only boolean, so we assert on type/behavior, not value exposure.
    }
}
