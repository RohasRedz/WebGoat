package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Delta tests for Assignment5 focusing on the SQL injection remediation:
 * ensuring that a PreparedStatement is used and that special-character inputs
 * do not cause SQL syntax errors while preserving logical behavior.
 */
public class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment5;

    @BeforeEach
    void setUp() {
        dataSource = mock(LessonDataSource.class);
        flags = mock(Flags.class);
        assignment5 = new Assignment5(dataSource, flags);
    }

    @Test
    void login_shouldSucceedForValidLarryCredentials_usingPreparedStatement() throws Exception {
        String username = "Larry";
        String password = "password123";

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        AttackResult result = assignment5.login(username, password);

        assertTrue(result.getLessonCompleted(), "Valid credentials for Larry should solve the challenge");

        // Verify PreparedStatement is used with parameter binding (no concatenation)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertTrue(usedSql.toLowerCase().contains("userid = ?"), "SQL must use parameter placeholder for userid");
        assertTrue(usedSql.toLowerCase().contains("password = ?"), "SQL must use parameter placeholder for password");

        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    void login_shouldFailForInvalidCredentials() throws Exception {
        String username = "Larry";
        String password = "wrong";

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        AttackResult result = assignment5.login(username, password);

        assertFalse(result.getLessonCompleted(), "Invalid credentials should not solve the challenge");
    }

    @Test
    void login_shouldHandleSqlMetaCharactersWithoutSyntaxError() throws Exception {
        String username = "Larry";
        // Input containing characters that would previously break concatenated SQL
        String password = "' OR '1'='1; --";

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // The main verification is that this call does NOT throw SQLException
        AttackResult result = assignment5.login(username, password);

        assertFalse(result.getLessonCompleted(), "Injection-style password should not bypass authentication");

        // Ensure the password is bound as a parameter, not concatenated
        verify(preparedStatement).setString(2, password);
    }

    @Test
    void login_shouldReturnGenericFailureOnDatabaseError() throws Exception {
        String username = "Larry";
        String password = "password123";

        when(dataSource.getConnection()).thenThrow(new SQLException("DB down"));

        AttackResult result = assignment5.login(username, password);

        assertFalse(result.getLessonCompleted(), "Database errors should result in failed challenge, not completion");
    }
}
