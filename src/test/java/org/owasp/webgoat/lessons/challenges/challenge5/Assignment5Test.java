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
 * Delta tests focused only on the changed behavior in Assignment5:
 * - SQL query now uses parameterized PreparedStatement instead of string concatenation.
 *
 * These tests verify:
 * 1) Login with valid 'Larry' credentials succeeds.
 * 2) Login with invalid credentials fails.
 * 3) SQL injection attempts in username or password do NOT cause authentication success.
 *
 * Implementation notes:
 * - We mock LessonDataSource/Connection/PreparedStatement/ResultSet to avoid any real DB access.
 * - We also mock Flags because Assignment5 depends on it to build success results.
 */
class Assignment5Test {

    private Assignment5 createAssignment5WithMocks(
            LessonDataSource dataSource,
            Flags flags) {
        return new Assignment5(dataSource, flags);
    }

    @Test
    @DisplayName("login should succeed for valid Larry credentials")
    void login_withValidLarryCredentials_succeeds() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
            .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = createAssignment5WithMocks(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "correct-password");

        // Assert
        // Success path must be taken
        // We only assert the generic outcome; feedback key is internal to WebGoat.
        // SUCCESS is indicated by isLessonCompleted / isSuccessful helper in AttackResult,
        // but without its implementation we rely on the presence of flag usage as proxy.
        // TODO: Replace this with stronger assertions if AttackResult API is available in the test scope.
        assertEquals("FLAG-5", result.getFeedbackArgs()[0]);

        // Also verify that parameters were bound correctly (no string concatenation)
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> pwdCaptor = ArgumentCaptor.forClass(String.class);
        verify(preparedStatement, times(1)).setString(eq(1), userCaptor.capture());
        verify(preparedStatement, times(1)).setString(eq(2), pwdCaptor.capture());
        assertEquals("Larry", userCaptor.getValue());
        assertEquals("correct-password", pwdCaptor.getValue());
    }

    @Test
    @DisplayName("login should fail for non-Larry username even if DB returns a row")
    void login_withInvalidUsername_failsBeforeDbQuery() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = createAssignment5WithMocks(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Mallory", "any-password");

        // Assert
        // Because the method explicitly checks for "Larry", it must fail before hitting the DB.
        // We assert that dataSource.getConnection() was never called to confirm the behavior
        // is preserved and not impacted by the SQL fix.
        verifyNoInteractions(dataSource);

        // TODO: Assert the specific feedback key if AttackResult exposes it in test scope.
        // For now we just ensure there is no flag argument attached for failure path.
        assertEquals(0, result.getFeedbackArgs().length);
    }

    @Test
    @DisplayName("login should NOT succeed for SQL injection attempt in password")
    void login_withSqlInjectionPassword_doesNotBypassAuthentication() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
            .thenReturn(preparedStatement);
        // Simulate DB returning no rows for malicious password
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = createAssignment5WithMocks(dataSource, flags);

        String maliciousPassword = "' OR '1'='1";

        // Act
        AttackResult result = assignment5.login("Larry", maliciousPassword);

        // Assert
        // The injection should NOT succeed; DB returns no rows, so result should be failure.
        // We assert no flag is provided as success feedback.
        assertEquals(0, result.getFeedbackArgs().length);

        // Verify that the user input is bound as a parameter, meaning no string concatenation
        ArgumentCaptor<String> pwdCaptor = ArgumentCaptor.forClass(String.class);
        verify(preparedStatement, times(1)).setString(eq(2), pwdCaptor.capture());
        assertEquals(maliciousPassword, pwdCaptor.getValue());
    }

    @Test
    @DisplayName("login should NOT succeed for SQL injection attempt in username")
    void login_withSqlInjectionUsername_doesNotBypassAuthentication() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
            .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = createAssignment5WithMocks(dataSource, flags);

        String maliciousUsername = "Larry' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(maliciousUsername, "any-password");

        // Assert
        // Because the code checks username equality to "Larry" *before* the DB call,
        // this should be treated as an invalid user, and DB should not be queried at all.
        verifyNoInteractions(dataSource);
        assertEquals(0, result.getFeedbackArgs().length);
    }
}
