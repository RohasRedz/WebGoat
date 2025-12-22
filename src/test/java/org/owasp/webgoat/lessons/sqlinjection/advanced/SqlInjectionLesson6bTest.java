package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests focusing on the security fixes in SqlInjectionLesson6b:
 * - Removed hard-coded default password ("dave") and now rely solely on DB value.
 * - Replaced printStackTrace() with structured logging. We focus on behavioral impact:
 *   when the password cannot be retrieved, completed() must fail instead of succeeding
 *   via a hard-coded fallback.
 */
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("completed() should fail when password cannot be retrieved (no hard-coded fallback)")
    void completedFailsWhenPasswordCannotBeRetrieved() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        // Simulate that creating a statement or executing the query throws an exception
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new RuntimeException("DB unavailable"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        // getPassword() will return null due to exception; completed() must handle null safely
        var result = lesson.completed("dave");

        // Assert
        assertFalse(result.getLessonCompleted(), "Lesson should not be marked as completed when password retrieval fails");
    }

    @Test
    @DisplayName("completed() should succeed only when DB password matches supplied userid_6b")
    void completedSucceedsOnlyWhenDbPasswordMatches() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("securePwd");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act & Assert
        // 1) Correct password -> success
        var successResult = lesson.completed("securePwd");
        assertTrue(successResult.getLessonCompleted(), "Lesson should be completed when user input matches DB password");

        // 2) Incorrect password -> failure (no fallback to any hard-coded value)
        var failureResult = lesson.completed("dave"); // original hard-coded default
        assertFalse(failureResult.getLessonCompleted(),
                "Lesson must not succeed based on any previous hard-coded default password");
    }
}
