// Assumed package based on source path; adjust if actual package differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta unit tests for Assignment5 focusing only on the changed behavior:
 * - The SQL login flow now uses a parameterized PreparedStatement instead of string concatenation.
 * These tests verify that:
 *   1) Parameters are bound via PreparedStatement#setString (secure behavior).
 *   2) Successful authentication for the expected user still works.
 *   3) Invalid credentials correctly fail.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login should authenticate Larry with correct password using parameterized query")
    void login_shouldAuthenticateLarryWithCorrectPassword_usingParameterizedQuery() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // simulate successful match
        when(flags.getFlag(5)).thenReturn("FLAG5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "secret");

        // Assert
        // Verify secure behavior: PreparedStatement with bound parameters is used
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "secret");

        // And the functional behavior is preserved (success path)
        assertTrue(result.isLessonCompleted(), "Expected lesson to be completed for valid credentials");
    }

    @Test
    @DisplayName("login should fail for invalid credentials while still using parameterized query")
    void login_shouldFailForInvalidCredentials_usingParameterizedQuery() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // simulate no match

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "wrong-password");

        // Assert
        // Still ensure parameterized query is used
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "wrong-password");

        assertTrue(result.getFeedback().orElse("").contains("challenge.close"),
                "Expected failure feedback for invalid credentials");
    }

    @Test
    @DisplayName("login should reject non-Larry usernames before hitting database")
    void login_shouldRejectNonLarryUsernames_withoutExecutingQuery() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Bob", "whatever");

        // Assert
        // For non-Larry users, DB should not be used at all (behavioral guard unchanged by the fix)
        verify(dataSource, never()).getConnection();
        assertTrue(result.getFeedback().orElse("").contains("user.not.larry"));
    }
}
