// TODO: Package inferred from source path; adjust if actual package differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on changed behavior:
 * - SQL is now parameterized via PreparedStatement (no string concatenation)
 * - Correct binding of parameters and handling of success/failure and SQLException
 */
public class Assignment5Test {

    @Test
    void login_ShouldUsePreparedStatementWithParameters_AndSolveChallengeOnMatch() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry";
        String password = "somePassword";

        // Act
        AttackResult result = assignment.login(username, password);

        // Assert - ensure parameterized SQL is used
        verify(connection, times(1)).prepareStatement(argThat(sql ->
                sql.contains("userid = ?") &&
                sql.contains("password = ?") &&
                !sql.contains(username) &&
                !sql.contains(password)));

        // Ensure parameters are bound correctly and user input not concatenated into SQL string
        verify(preparedStatement, times(1)).setString(1, username);
        verify(preparedStatement, times(1)).setString(2, password);

        // Behavior: challenge solved when a row exists
        assertThat(result).isNotNull();
        assertThat(result.isLessonSolved()).isTrue();
    }

    @Test
    void login_WhenNoResult_ShouldReturnFailed() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment.login("Larry", "wrongPassword");

        // Assert: lesson should not be solved
        assertThat(result).isNotNull();
        assertThat(result.isLessonSolved()).isFalse();
    }

    @Test
    void login_WhenSQLExceptionOccurs_ShouldReturnFailedWithoutLeakingDetails() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        // Act
        AttackResult result = assignment.login("Larry", "any");

        // Assert: generic failure; no exception propagated
        assertThat(result).isNotNull();
        assertThat(result.isLessonSolved()).isFalse();
    }

    @Test
    void login_WhenUserIsNotLarry_ShouldFailEarly_WithoutDBInteraction() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment.login("Bob", "pwd");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isLessonSolved()).isFalse();
        verifyNoInteractions(dataSource);
    }

    @Test
    void login_WhenUsernameOrPasswordBlank_ShouldFailEarly_WithoutDBInteraction() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        // Act
        AttackResult result1 = assignment.login("", "pwd");
        AttackResult result2 = assignment.login("Larry", "");

        // Assert
        assertThat(result1.isLessonSolved()).isFalse();
        assertThat(result2.isLessonSolved()).isFalse();
        verifyNoInteractions(dataSource);
    }
}
