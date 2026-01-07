package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Delta unit tests focusing on the secure behavior introduced in Assignment5:
 * - PreparedStatement with parameterized query (no SQL concatenation)
 * - Existing behavior for valid, invalid, and missing credentials is preserved.
 */
class Assignment5Test {

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
    }

    @Test
    void login_withValidLarryCredentials_usesParameterizedQueryAndSucceeds() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG5");

        AttackResult result = assignment5.login("Larry", "password123");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        // Assert that the SQL string no longer concatenates user input
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                sql,
                "SQL should use parameter placeholders");

        // Assert that parameters are bound correctly
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "password123");

        // Behavior: still succeeds for correct credentials
        assertTrue(result.isCorrect(), "Login should succeed for valid Larry credentials");
    }

    @Test
    void login_withMaliciousInput_doesNotInjectSqlAndFailsCleanly() throws Exception {
        // Even if DB returns a row, we verify that parameters are passed as data, not SQL.
        when(resultSet.next()).thenReturn(false); // Simulate invalid credentials

        String evilUsername = "Larry' OR '1'='1";
        String evilPassword = "anything' OR '1'='1";

        AttackResult result = assignment5.login(evilUsername, evilPassword);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                sql,
                "SQL must remain parameterized even for malicious input");
        verify(preparedStatement).setString(1, evilUsername);
        verify(preparedStatement).setString(2, evilPassword);

        assertTrue(result.isError() || !result.isCorrect(),
                "Login should not be treated as successful for SQL injection attempts");
    }

    @Test
    void login_withInvalidUser_preservesExistingFailureBehavior() throws Exception {
        AttackResult result = assignment5.login("NotLarry", "somePassword");

        // query must never be executed for non-Larry user
        verify(connection, never()).prepareStatement(anyString());
        verify(preparedStatement, never()).executeQuery();

        assertTrue(result.isError() || !result.isCorrect(),
                "Non-Larry users should still fail before reaching SQL execution");
    }

    @Test
    void login_withMissingParameters_returnsRequiredError() throws Exception {
        AttackResult result1 = assignment5.login("", "password");
        AttackResult result2 = assignment5.login("Larry", "");

        // In both cases, query must not execute
        verify(connection, never()).prepareStatement(anyString());

        assertTrue(result1.isError() || !result1.isCorrect(),
                "Empty username should be rejected");
        assertTrue(result2.isError() || !result2.isCorrect(),
                "Empty password should be rejected");
    }
}
