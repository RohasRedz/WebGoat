package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
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
 * Delta tests focused on the secure SQL parameterization in Assignment5.login().
 *
 * These tests verify that:
 * - The SQL query uses parameter placeholders (no user input concatenated into SQL text).
 * - The username and password are passed via PreparedStatement parameters.
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
        when(resultSet.next()).thenReturn(true); // simulate successful login
        when(flags.getFlag(5)).thenReturn("FLAG-5");
    }

    @Test
    void login_usesParameterizedQueryAndBindsUserInputs() throws Exception {
        String username = "Larry";
        String password = "secretPassword";

        AttackResult result = assignment5.login(username, password);

        // Verify that a query with parameter placeholders is used
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        // The query should contain placeholders for userid and password
        // and must not contain raw user-supplied values.
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL should use parameter placeholders");
        org.junit.jupiter.api.Assertions.assertFalse(
                usedSql.contains(username) || usedSql.contains(password),
                "SQL must not embed raw user input");

        // Verify that the parameters are bound via setString
        verify(preparedStatement).setString(1, eq(username));
        verify(preparedStatement).setString(2, eq(password));

        // Sanity check: behavior still returns success when resultSet.next() is true
        assertEquals("success", result.getLessonStatus().name().toLowerCase());
    }
}
