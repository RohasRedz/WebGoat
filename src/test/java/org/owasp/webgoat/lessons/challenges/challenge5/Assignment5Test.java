package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for {@link Assignment5} focused on the SQL injection fix.
 *
 * Before: SQL query was constructed via string concatenation including user input.
 * After:  The query uses parameter placeholders with setString(…) binding.
 *
 * These tests verify:
 *  - Correct use of parameterized query (no concatenation).
 *  - Parameters are bound in the correct order and values.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized query and bind username and password safely")
    void loginUsesPreparedStatementParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "anyPassword' OR '1'='1"; // typical injection payload – should be safe now

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Act
        AttackResult result = assignment.login(username, password);

        // Assert
        // Verify proper parameter binding (order and value) instead of string concatenation
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isTrue();
    }

    @Test
    @DisplayName("login should still fail when credentials are incorrect, even with parameterized query")
    void loginWithWrongCredentialsStillFails() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "wrongPassword";

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment.login(username, password);

        // Assert
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
