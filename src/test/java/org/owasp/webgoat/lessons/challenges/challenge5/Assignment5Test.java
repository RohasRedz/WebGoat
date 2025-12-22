package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta unit tests for Assignment5 focusing only on the changed behavior:
 * - SQL query is now parameterized (uses ? placeholders) instead of string concatenation.
 * - Authentication behavior remains correct when valid credentials are provided.
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
        dataSource = Mockito.mock(LessonDataSource.class);
        flags = Mockito.mock(Flags.class);

        assignment5 = new Assignment5(dataSource, flags);

        connection = Mockito.mock(Connection.class);
        preparedStatement = Mockito.mock(PreparedStatement.class);
        resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void login_usesParameterizedQueryAndBindsUserInputs() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "secret";
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: query string uses placeholders instead of concatenated input
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        // Ensure no raw user input is directly concatenated; must contain ? placeholders
        // and not literal username/password
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL should use parameter placeholders for userid and password");
        org.junit.jupiter.api.Assertions.assertFalse(
                usedSql.contains(username) || usedSql.contains(password),
                "SQL must not contain raw user input directly concatenated into the query");

        // Assert: parameters are actually bound in the correct order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Assert: behavior remains success when resultSet has a row
        assertEquals(AttackResult.Status.SUCCESS, result.getStatus());
        verify(resultSet, times(1)).next();
    }

    @Test
    void login_failsWhenNoRowEvenWithParameterizedQuery() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "wrong";
        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: still uses prepared statement and bound parameters
        verify(connection).prepareStatement(anyString());
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Behavior: failure when no row is returned
        assertEquals(AttackResult.Status.FAIL, result.getStatus());
    }
}
