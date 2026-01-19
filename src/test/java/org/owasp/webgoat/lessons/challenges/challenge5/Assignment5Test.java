package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

import static org.mockito.Mockito.*;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix:
 * - Ensure PreparedStatement with parameter placeholders is used.
 * - Ensure user input is bound via setString and not concatenated into the SQL.
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
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(flags.getFlag(5)).thenReturn("FLAG-5");
        when(resultSet.next()).thenReturn(true);

        assignment5 = new Assignment5(dataSource, flags);
    }

    @Test
    void login_usesParameterizedQueryAndBindsUserInput() throws Exception {
        String username = "Larry";
        String password = "safePassword123";

        AttackResult result = assignment5.login(username, password);

        // Assert we queried successfully (happy path preserved)
        assertEquals(
                success(assignment5).feedback("challenge.solved").feedbackArgs("FLAG-5").build().getLesson(),
                result.getLesson());

        // Capture SQL and parameters used on PreparedStatement
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        // Ensure SQL now uses placeholders instead of direct concatenation
        String expectedSql =
                "select password from challenge_users where userid = ? and password = ?";
        assertEquals(expectedSql, sql);

        // Ensure user-supplied values are bound via setString
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    void login_doesNotExposeSqlInjectionViaConcatenation() throws Exception {
        String maliciousUsername = "Larry' OR '1'='1";
        String maliciousPassword = "anything";

        when(resultSet.next()).thenReturn(false); // ensure logic result does not matter for this delta test

        assignment5.login(maliciousUsername, maliciousPassword);

        // Ensure the SQL statement text is constant and not containing raw user input
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        // The fixed query must not contain the raw malicious payload
        // (user input must only appear in bound parameters)
        org.junit.jupiter.api.Assertions.assertFalse(sql.contains(maliciousUsername));
        org.junit.jupiter.api.Assertions.assertFalse(sql.contains(maliciousPassword));

        // Verify parameters are still bound correctly
        verify(preparedStatement).setString(1, maliciousUsername);
        verify(preparedStatement).setString(2, maliciousPassword);
    }
}
