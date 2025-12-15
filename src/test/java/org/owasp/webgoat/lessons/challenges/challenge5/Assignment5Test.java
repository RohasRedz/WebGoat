package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
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
import org.springframework.web.bind.MissingServletRequestParameterException;

/**
 * Delta tests for Assignment5 focusing on the fixed SQL injection behavior.
 *
 * These tests:
 * - Verify that the query is executed with bound parameters (PreparedStatement#setString)
 *   instead of concatenated user input.
 * - Verify that correct credentials still succeed and incorrect/malicious ones fail.
 *
 * NOTE: We do not attempt to reproduce the vulnerable behavior; instead we assert
 * the secure, parameterized behavior that replaced it.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login with correct credentials uses parameter binding and succeeds")
    void loginWithValidCredentials_usesParametersAndSucceeds() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "secret";

        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1) Still succeeds for correct credentials
        assertTrue(result.getLessonsCompletedHint().contains("challenge.solved"));

        // 2) Verify that parameters are bound via setString and user input is not concatenated into SQL
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());

        String usedSql = sqlCaptor.getValue();
        // The fixed query must contain parameter placeholders:
        assertTrue(usedSql.contains("userid = ?"), "Expected userid parameter placeholder in SQL");
        assertTrue(usedSql.contains("password = ?"), "Expected password parameter placeholder in SQL");
        // Ensure raw user values are not present in the SQL string
        assertTrue(!usedSql.contains(username), "Username must not be concatenated into SQL");
        assertTrue(!usedSql.contains(password), "Password must not be concatenated into SQL");

        // Verify parameter binding order and values
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    @DisplayName("login with malicious SQL payload does not bypass authentication")
    void loginWithSqlInjectionPayload_doesNotBypassAuth() throws Exception {
        // Arrange
        String username = "Larry";
        // Classic SQL injection payload that previously could break the WHERE clause
        String maliciousPassword = "' OR '1'='1";

        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Simulate that the database does NOT find a row for the malicious password
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login(username, maliciousPassword);

        // Assert
        // Authentication must fail; SQL injection must not cause success
        assertTrue(result.getLessonsCompletedHint().contains("challenge.close"));

        // And the SQL statement must still use parameter placeholders (no concatenation)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());

        String usedSql = sqlCaptor.getValue();
        assertTrue(usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL must use parameter placeholders");
        // Malicious payload must not appear directly in the SQL text
        assertTrue(!usedSql.contains(maliciousPassword), "Malicious payload must not be concatenated into SQL");

        // Verify that the malicious payload is passed only as a bound parameter
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, maliciousPassword);
    }

    @Test
    @DisplayName("login with missing or blank parameters still fails fast (unchanged behavior gate around the fix)")
    void loginWithBlankParameters_failsFast() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult resultEmptyUser = assignment5.login("", "somePassword");
        AttackResult resultEmptyPassword = assignment5.login("Larry", "");

        // Assert
        // These paths are not part of the vulnerability fix itself but guard the SQL branch.
        assertTrue(resultEmptyUser.getLessonsCompletedHint().contains("required4"));
        assertTrue(resultEmptyPassword.getLessonsCompletedHint().contains("required4"));
    }

    // NOTE: We do not test HTTP wiring, only the endpoint method behavior and its interaction
    // with JDBC objects as affected by the SQL injection fix.
}
