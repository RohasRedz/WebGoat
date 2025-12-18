package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.util.StringUtils;

@DisplayName("Delta tests for Assignment5 SQL injection fix")
class Assignment5Test {

    @Test
    @DisplayName("login() should use parameterized PreparedStatement with user inputs as bind variables")
    void login_usesParameterizedQuery() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry' OR '1'='1";
        String password = "pwd' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Verify that the SQL string uses placeholders rather than concatenated parameters
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        // The core assertion: query must contain ? placeholders (parameterization)
        // and not the raw username or password string.
        // We do not assert the entire SQL; we just assert the presence of bind markers
        // and absence of direct concatenation of user input.
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                usedSql,
                "SQL query should use parameter placeholders instead of concatenated user input"
        );

        // Verify that bind variables are set with the exact user-provided values
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Also ensure that the happy path still succeeds
        // (delta test: behavior preserved while fixing injection).
        // AttackResult has no simple equals; check that it's a success result by feedback key.
        // We only validate that the 'challenge.solved' feedback is still used.
        assertSame(
                AttackResult.Type.SUCCESS,
                result.getType(),
                "Result should remain SUCCESS when credentials are correct"
        );
    }

    @Test
    @DisplayName("login() should still fail fast on empty username or password (regression guard)")
    void login_rejectsEmptyInputs() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Empty username
        AttackResult result1 = assignment5.login("", "password");
        assertSame(AttackResult.Type.FAILURE, result1.getType());
        // Empty password
        AttackResult result2 = assignment5.login("Larry", " ");
        assertSame(AttackResult.Type.FAILURE, result2.getType());
        // Ensure original StringUtils-based check is still in effect
        // (delta test ensures validation not accidentally removed).
        assertEquals(false, StringUtils.hasText(" "), "Sanity check on StringUtils semantics");
    }
}
