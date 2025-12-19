package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on:
 * 1) SQL is parameterized and does not concatenate username/password.
 * 2) Behavior for valid Larry credentials vs invalid credentials is preserved.
 */
class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized PreparedStatement and not concatenate user input")
    void loginShouldUseParameterizedQueryWithoutConcatenation() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("flag-5");

        String username = "Larry";
        String password = "somePassword";

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        assignment5.login(username, password);

        // Assert
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        // The SQL must contain placeholders and must not contain the raw username or password.
        assertThat(usedSql)
                .as("SQL should use placeholders for userid and password")
                .contains("userid = ?")
                .contains("password = ?");

        assertThat(usedSql)
                .as("SQL should not contain raw username")
                .doesNotContain(username);

        assertThat(usedSql)
                .as("SQL should not contain raw password")
                .doesNotContain(password);

        // Also verify that bind parameters are set correctly.
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    @DisplayName("login should succeed for Larry when query returns a row")
    void loginShouldSucceedForLarryWhenRowExists() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("flag-5");

        String username = "Larry";
        String password = "correctPassword";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertThat(result)
                .as("AttackResult should indicate success when credentials match a row")
                .matches(AttackResult::getLessonCompleted);
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    @DisplayName("login should fail when Larry's credentials do not match any row")
    void loginShouldFailWhenNoRowForLarry() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        String username = "Larry";
        String password = "wrongPassword";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertThat(result)
                .as("AttackResult should indicate failure when no matching row is found")
                .matches(r -> !r.getLessonCompleted());
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    @DisplayName("login should fail early for non-Larry user, without executing SQL")
    void loginShouldFailForNonLarryWithoutSqlExecution() throws Exception {
        // This test ensures original behavior is preserved and also guarantees
        // that the SQL path is not used for non-Larry users.
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Alice";
        String password = "anyPassword";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertThat(result)
                .as("AttackResult should indicate failure for non-Larry user")
                .matches(r -> !r.getLessonCompleted());
        // Ensure no DB interaction for non-Larry
        verify(dataSource, never()).getConnection();
    }
}
