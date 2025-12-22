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
 * Delta unit tests for Assignment5 focusing on the SQL injection fix:
 * - Ensure PreparedStatement parameters are correctly bound.
 * - Ensure SQL injection-like password does not bypass authentication.
 */
public class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        flags = mock(Flags.class);
        assignment = new Assignment5(dataSource, flags);

        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(flags.getFlag(5)).thenReturn("FLAG-5");
    }

    @Test
    void login_withValidLarryCredentials_succeedsAndUsesParameterizedQuery() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "secret";
        when(resultSet.next()).thenReturn(true);

        // Act
        AttackResult result = assignment.login(username, password);

        // Assert: behavior unchanged – success on correct credentials
        // (AssignmentResult is part of framework; we assert via message key and contained flag)
        assertEquals("FLAG-5", result.getFeedbackArgs()[0]);

        // Assert: SQL uses parameter binding and not concatenation
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        // Ensure the SQL uses placeholders and not raw user input
        // (no 'Larry' or 'secret' should appear in the SQL string)
        org.junit.jupiter.api.Assertions.assertTrue(sql.contains("userid = ?"),
                "SQL must use parameter placeholder for userid");
        org.junit.jupiter.api.Assertions.assertTrue(sql.contains("password = ?"),
                "SQL must use parameter placeholder for password");
        org.junit.jupiter.api.Assertions.assertFalse(sql.contains(username),
                "SQL must not contain raw username");
        org.junit.jupiter.api.Assertions.assertFalse(sql.contains(password),
                "SQL must not contain raw password");

        // Assert: parameters were actually bound in order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    void login_withSqlInjectionLikePassword_doesNotBypassAuthentication() throws Exception {
        // Arrange
        String username = "Larry";
        String injectionPassword = "anything' OR '1'='1";
        when(resultSet.next()).thenReturn(false); // No row returned despite injection-like input

        // Act
        AttackResult result = assignment.login(username, injectionPassword);

        // Assert: authentication fails; injection does not cause success
        // We assert by checking that feedback message key is the failure one ("challenge.close")
        assertEquals("challenge.close", result.getFeedbackId());

        // Also verify parameters are still bound safely and not concatenated into SQL
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, injectionPassword);
    }

    @Test
    void login_withNonLarryUser_stillFailsBeforeDbAccess() throws Exception {
        // Arrange
        String username = "Bob";
        String password = "irrelevant";

        // Act
        AttackResult result = assignment.login(username, password);

        // Assert: user not Larry still fails, and DB is never called
        assertEquals("user.not.larry", result.getFeedbackId());
        verifyNoInteractions(connection);
        verifyNoInteractions(preparedStatement);
    }
}
