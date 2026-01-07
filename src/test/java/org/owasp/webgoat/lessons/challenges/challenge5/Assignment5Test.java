/*
 * Delta test for BATCH-001 - Assignment5.java
 */
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/**
 * Delta tests for Assignment5 focusing on the secure PreparedStatement usage.
 * These tests ensure that user-supplied values are bound as parameters instead of being
 * concatenated into the SQL string, verifying the fix for SQL injection vulnerability.
 *
 * Jira: SVCF-1808
 */
public class Assignment5Test {

    @Test
    @DisplayName("login() should use parameterized PreparedStatement with username and password")
    void login_usesPreparedStatementParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("flag-5");

        String username = "Larry";
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1) Ensure the SQL string no longer contains raw concatenated user inputs
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sqlUsed = sqlCaptor.getValue();

        // The SQL should be parameterized; it must contain '?' placeholders
        // and must not literally contain the username or password.
        org.junit.jupiter.api.Assertions.assertTrue(
                sqlUsed.contains("userid = ?") && sqlUsed.contains("password = ?"),
                "SQL should use parameter placeholders for userid and password");
        org.junit.jupiter.api.Assertions.assertFalse(
                sqlUsed.contains(username) || sqlUsed.contains(password),
                "SQL must not directly contain user-supplied values");

        // 2) Verify that PreparedStatement parameters are set correctly
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        // 3) Verify successful login result preserved
        assertEquals(true, result.isLessonCompleted(), "Login should still succeed for valid credentials");
    }

    @Test
    @DisplayName("login() should reject non-Larry usernames (unchanged behavior guard)")
    void login_rejectsNonLarryUsernames() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("NotLarry", "any");

        org.junit.jupiter.api.Assertions.assertFalse(result.isLessonCompleted());
    }

    @Test
    @DisplayName("login() should fail when username or password is blank (input validation unchanged)")
    void login_rejectsBlankInputs() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result1 = assignment5.login("", "pw");
        AttackResult result2 = assignment5.login("Larry", "");

        org.junit.jupiter.api.Assertions.assertFalse(result1.isLessonCompleted());
        org.junit.jupiter.api.Assertions.assertFalse(result2.isLessonCompleted());
    }
}
