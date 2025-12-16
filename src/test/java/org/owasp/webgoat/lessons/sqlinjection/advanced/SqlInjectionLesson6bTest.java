package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests focusing on:
 * - completed() delegating to checkPasswordForDave().
 * - Success when password matches, failure otherwise.
 * - Raw password is never exposed; only a boolean outcome is used.
 */
class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        lesson = spy(new SqlInjectionLesson6b(dataSource));

        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    @DisplayName("completed() delegates to checkPasswordForDave and returns success if it returns true")
    void completed_delegatesToCheckPasswordForDave_success() throws IOException {
        // Arrange
        String providedPassword = "correct-password";
        doReturn(true).when(lesson).checkPasswordForDave(providedPassword);

        // Act
        AttackResult result = lesson.completed(providedPassword);

        // Assert
        verify(lesson).checkPasswordForDave(providedPassword);
        assertTrue(result.getLessonCompleted(), "Expected lesson to be completed when password is correct");
    }

    @Test
    @DisplayName("completed() delegates to checkPasswordForDave and returns failure if it returns false")
    void completed_delegatesToCheckPasswordForDave_failure() throws IOException {
        // Arrange
        String providedPassword = "wrong-password";
        doReturn(false).when(lesson).checkPasswordForDave(providedPassword);

        // Act
        AttackResult result = lesson.completed(providedPassword);

        // Assert
        verify(lesson).checkPasswordForDave(providedPassword);
        assertFalse(result.getLessonCompleted(), "Expected lesson not to be completed when password is incorrect");
    }

    @Test
    @DisplayName("checkPasswordForDave uses PreparedStatement and never exposes raw password")
    void checkPasswordForDave_usesPreparedStatement_andDoesNotExposePassword() throws SQLException {
        // Arrange
        String providedPassword = "any-pass";
        when(resultSet.next()).thenReturn(true);

        // Act
        boolean matches = lesson.checkPasswordForDave(providedPassword);

        // Assert: verify SQL structure and parameter binding
        verify(connection).prepareStatement(
                "SELECT password FROM user_system_data WHERE user_name = 'dave' AND password = ?");
        verify(preparedStatement).setString(1, providedPassword);
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();

        assertTrue(matches, "Expected true when resultSet returns at least one row");
        // No assertion on raw password value because it is never returned by the method.
    }
}
