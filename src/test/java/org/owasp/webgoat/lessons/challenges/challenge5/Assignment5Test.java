package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Delta tests focused on the change from string-concatenated SQL to a parameterized
 * PreparedStatement in Assignment5.login().
 */
class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized query and returns success for correct credentials")
    void login_usesParameterizedQuery_andSucceedsForValidCredentials() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry";
        String password = "password123";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: Prepared statement must be called with the parametrized SQL
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        // Assert: Parameters are bound, not concatenated
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Ensure no evidence of concatenated SQL by checking that setString was called
        // with the raw user input (no quotes) and that prepareStatement wasn't invoked
        // with a concatenated string.
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        // The SQL should not contain embedded user values
        assertEquals("select password from challenge_users where userid = ? and password = ?", usedSql);

        // Assert: Successful login path still works
        // challenge.solved feedback is used on success; we just ensure result is marked as success
        // without needing to know the exact internal representation.
        // AttackResult does not expose a direct "success" flag, so we validate via its string form.
        String resultString = result.toString();
        // Should at least contain the success feedback key
        org.junit.jupiter.api.Assertions.assertTrue(resultString.contains("challenge.solved"));
    }

    @Test
    @DisplayName("login fails when credentials do not match any row (parameterized query still used)")
    void login_usesParameterizedQuery_andFailsForInvalidCredentials() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        String username = "Larry";
        String password = "wrong-password";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: query is still parametrized
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Assert: failure feedback is returned for invalid credentials
        String resultString = result.toString();
        org.junit.jupiter.api.Assertions.assertTrue(resultString.contains("challenge.close"));
    }
}
