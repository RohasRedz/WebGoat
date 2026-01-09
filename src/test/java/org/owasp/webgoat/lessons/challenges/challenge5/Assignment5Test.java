package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Delta tests for Assignment5 focusing on the fixed SQL injection behavior.
 *
 * These tests verify:
 * - The login method uses a parameterized PreparedStatement (no SQL concatenation with user input).
 * - Success and failure paths for valid/invalid credentials still behave correctly.
 */
class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
        flags = org.mockito.Mockito.mock(Flags.class);
        assignment = new Assignment5(dataSource, flags);

        connection = org.mockito.Mockito.mock(Connection.class);
        preparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
        resultSet = org.mockito.Mockito.mock(ResultSet.class);

        org.mockito.Mockito.when(dataSource.getConnection()).thenReturn(connection);
        org.mockito.Mockito.when(
                        connection.prepareStatement(
                                org.mockito.Mockito.anyString()))
                .thenReturn(preparedStatement);
        org.mockito.Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        org.mockito.Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");
    }

    @Test
    void login_usesPreparedStatementWithBoundParameters_onSuccess() throws Exception {
        // Arrange: simulate a successful login
        org.mockito.Mockito.when(resultSet.next()).thenReturn(true);

        // Act
        AttackResult result = assignment.login("Larry", "secret");

        // Assert: success path still works
        assertTrue(result.getLessonCompleted(), "Expected challenge to be marked as solved");

        // Assert: query string still generic, without concatenated user input
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        // Ensure the SQL uses placeholders instead of concatenating user input
        org.junit.jupiter.api.Assertions.assertTrue(
                sql.contains("userid = ?") && sql.contains("password = ?"),
                "SQL should use parameter placeholders for userid and password");

        // Assert: parameter binding uses user-supplied values, ensuring no concatenation
        org.mockito.Mockito.verify(preparedStatement).setString(1, "Larry");
        org.mockito.Mockito.verify(preparedStatement).setString(2, "secret");
    }

    @Test
    void login_failsForIncorrectPassword_andStillUsesParameters() throws Exception {
        // Arrange: simulate authentication failure
        org.mockito.Mockito.when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment.login("Larry", "wrong");

        // Assert: should be failed result
        assertFalse(result.getLessonCompleted(), "Expected challenge not to be solved");

        // Assert: parameters are still bound correctly
        org.mockito.Mockito.verify(preparedStatement).setString(1, "Larry");
        org.mockito.Mockito.verify(preparedStatement).setString(2, "wrong");
    }

    @Test
    void login_failsForNonLarryUser_beforeReachingDatabase() throws Exception {
        // Act
        AttackResult result = assignment.login("Bob", "anything");

        // Assert: should fail fast and not hit the database
        assertFalse(result.getLessonCompleted(), "Non-Larry user should not pass the challenge");
        org.mockito.Mockito.verifyNoInteractions(dataSource);
    }

    @Test
    void login_failsWhenUsernameOrPasswordBlank() throws Exception {
        // Guard-rail tests ensuring validation logic still holds

        AttackResult resultEmptyUser = assignment.login("", "pwd");
        AttackResult resultEmptyPwd = assignment.login("Larry", "");
        AttackResult resultBothEmpty = assignment.login("", "");

        assertFalse(resultEmptyUser.getLessonCompleted());
        assertFalse(resultEmptyPwd.getLessonCompleted());
        assertFalse(resultBothEmpty.getLessonCompleted());
    }
}
