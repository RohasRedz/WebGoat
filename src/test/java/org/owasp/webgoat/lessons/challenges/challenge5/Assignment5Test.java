package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing only on the changed SQL behavior.
 * Verifies that a parameterized PreparedStatement is used and that parameters
 * are bound as expected for success/failure flows.
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
    void login_usesParameterizedQuery_andSucceedsForValidCredentials() throws Exception {
        String username = "Larry";
        String password = "secret";

        when(resultSet.next()).thenReturn(true);

        AttackResult result = assignment5.login(username, password);

        // Verify SQL uses placeholders instead of concatenated input
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        // The exact whitespace is not important; we care about the placeholders
        // and absence of user input concatenation.
        // This checks that the query contains parameter placeholders.
        org.junit.jupiter.api.Assertions.assertTrue(
                sql.toLowerCase().contains("userid = ?") && sql.toLowerCase().contains("password = ?"),
                "SQL should use parameter placeholders for userid and password");

        // Verify parameters are bound correctly and in correct order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        // Behavior: success branch when a row is returned
        org.junit.jupiter.api.Assertions.assertTrue(result.getLessonCompleted(), "Expected success when credentials match");
    }

    @Test
    void login_failsForInvalidCredentials_butStillUsesParameterizedQuery() throws Exception {
        String username = "Larry";
        String password = "wrong";

        when(resultSet.next()).thenReturn(false);

        AttackResult result = assignment5.login(username, password);

        // Still must use parameterized query for invalid credentials
        verify(connection).prepareStatement(anyString());
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        org.junit.jupiter.api.Assertions.assertFalse(result.getLessonCompleted(), "Expected failure when credentials are invalid");
    }

    @Test
    void login_rejectsSqlInjectionPayloadInsteadOfAlteringQueryStructure() throws Exception {
        String username = "Larry";
        String injectionPassword = "pw' OR '1'='1";

        when(resultSet.next()).thenReturn(false);

        AttackResult result = assignment5.login(username, injectionPassword);

        // The injection payload should not alter the SQL structure, only be bound as a parameter.
        verify(connection).prepareStatement(anyString());
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, injectionPassword);

        org.junit.jupiter.api.Assertions.assertFalse(result.getLessonCompleted(),
                "SQL injection payload must not cause authentication to succeed");
    }
}
