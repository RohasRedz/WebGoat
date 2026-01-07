package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Delta tests focused on the changed behavior in Assignment5:
 * - SQL query is now parameterized (no string concatenation).
 * - Validate that login succeeds/fails correctly and that prepared statement
 *   parameters are bound safely (basic SQL injection attempts are not executed).
 */
public class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment5;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        flags = mock(Flags.class);
        assignment5 = new Assignment5(dataSource, flags);

        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(flags.getFlag(5)).thenReturn("FLAG-5");
    }

    @Test
    void login_shouldUseParameterizedQuery_andSucceedForValidLarryUser() throws Exception {
        // Arrange
        when(resultSet.next()).thenReturn(true);
        String username = "Larry";
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertNotNull(result);
        assertEquals(AttackResult.Status.SUCCESS, result.getLessonStatus());

        // Verify that parameterized SQL with placeholders is used
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        // The updated code must contain '?' placeholders and not concatenate user input
        // The test ensures presence of placeholders - if the previous vulnerable behavior
        // (string concatenation) is reintroduced, this assertion will fail.
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL must use parameter placeholders instead of concatenating user input");

        // Verify that user input is bound via setString (no concatenation)
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    void login_shouldFailForInvalidLarryPassword() throws Exception {
        // Arrange
        when(resultSet.next()).thenReturn(false);
        String username = "Larry";
        String wrongPassword = "wrong";

        // Act
        AttackResult result = assignment5.login(username, wrongPassword);

        // Assert
        assertNotNull(result);
        assertEquals(AttackResult.Status.FAILED, result.getLessonStatus());
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, wrongPassword);
    }

    @Test
    void login_shouldResistBasicSqlInjectionAttemptInPassword() throws Exception {
        // Arrange
        // Even if the attacker tries to inject into the password field,
        // the use of prepared statements should treat it as data, not SQL.
        when(resultSet.next()).thenReturn(false);
        String username = "Larry";
        String maliciousPassword = "' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(username, maliciousPassword);

        // Assert
        assertNotNull(result);
        assertEquals(
                AttackResult.Status.FAILED,
                result.getLessonStatus(),
                "Basic SQL injection attempt should not bypass authentication");

        // Ensure the malicious input is passed as a bound parameter, not concatenated
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL must remain parameterized even for malicious input");

        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, maliciousPassword);
    }

    @Test
    void login_shouldFailWhenUsernameIsNotLarry_andNeverExecuteQuery() throws Exception {
        // Arrange
        String username = "Bob";
        String password = "irrelevant";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertNotNull(result);
        assertEquals(AttackResult.Status.FAILED, result.getLessonStatus());

        // For non-Larry usernames, the method should return early and not hit the DB
        verifyNoInteractions(connection);
    }
}
