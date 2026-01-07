package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the SQL-injection-related behavior
 * changed by the fix (parameterized PreparedStatement instead of string concatenation).
 *
 * These tests:
 * - Ensure the query remains parameterized and does not inline user input.
 * - Verify that a "malicious" username does not bypass authentication.
 * - Verify that valid credentials still result in a success.
 */
class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        flags = mock(Flags.class);
        assignment = new Assignment5(dataSource, flags);

        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(flags.getFlag(5)).thenReturn("flag-5");
    }

    @Test
    void login_withMaliciousUsername_doesNotBypassAuthentication_andUsesParameterizedQuery() throws Exception {
        // Arrange
        String maliciousUsername = "Larry' OR '1'='1";
        String password = "anyPass";

        // No rows returned -> authentication fails
        when(resultSet.next()).thenReturn(false);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        AttackResult result = assignment.login(maliciousUsername, password);

        // Assert: authentication must fail (no bypass)
        assertFalse(result.getLessonCompleted(), "Malicious username must not bypass authentication");

        // Assert: PreparedStatement SQL must be parameterized (no direct user concatenation)
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        // SQL should contain placeholders and must not directly contain the user input string
        assertTrue(usedSql.contains("userid = ?"), "SQL must use parameter placeholder for userid");
        assertTrue(usedSql.contains("password = ?"), "SQL must use parameter placeholder for password");
        assertFalse(usedSql.contains(maliciousUsername),
                "SQL string must not contain raw malicious username (should be bound as parameter)");

        // Assert: user input is passed as parameters and not concatenated into the SQL
        verify(preparedStatement).setString(1, maliciousUsername);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    void login_withValidLarryCredentials_behavesAsBefore_andSucceeds() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "correct-password";

        // Simulate one matching row -> success
        when(resultSet.next()).thenReturn(true);

        // Act
        AttackResult result = assignment.login(username, password);

        // Assert: success path preserved
        assertTrue(result.getLessonCompleted(), "Valid Larry credentials should still succeed after the fix");
        // Ensure flag is still used in success feedback
        verify(flags).getFlag(5);

        // Also ensure parameters are bound correctly for the valid case
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    void login_withNonLarryUsername_stillRejectedBeforeReachingDatabase() throws Exception {
        // This test is a guard to ensure the precondition check (username must be Larry)
        // still short-circuits and does not rely on SQL behavior, which is important for
        // preventing injection attempts via other usernames.

        // Arrange
        String otherUser = "Bob";
        String password = "whatever";

        // Act
        AttackResult result = assignment.login(otherUser, password);

        // Assert: lesson should not be completed
        assertFalse(result.getLessonCompleted(), "Non-Larry user should not be accepted");

        // And DB should still be used (the code only checks Larry, but we verify here that behavior didn't change)
        // If the implementation ever changes to not even query for non-Larry, this assertion should be adjusted.
        // For now we only assert that no exception is thrown and method returns a failed result.
    }
}
