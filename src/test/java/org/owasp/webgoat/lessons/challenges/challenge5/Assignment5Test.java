package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix.
 *
 * These tests verify, via behavior and JDBC interaction, that:
 * - Only the correct password authenticates (no injection bypass).
 * - A typical SQL injection payload in the password no longer authenticates.
 */
class Assignment5Test {

    @Test
    @DisplayName("login should succeed only with correct credentials and use bound parameters")
    void loginSucceedsOnlyWithCorrectCredentials() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true); // Simulate user row exists for valid credentials
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Act
        AttackResult result = assignment5.login("Larry", "correct-password");

        // Assert
        assertEquals("success", result.getLessonCompleted(), "Expected success for correct credentials");

        // Also assert that the prepared statement used parameters rather than concatenation
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(conn).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        // We ensure the SQL text contains placeholders and no raw user data
        // The exact string is implementation-dependent, so we check for '?' and key fragments
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.contains("challenge_users") && usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL must use parameter placeholders instead of concatenating user input");

        // Ensure parameters are bound as separate values
        verify(ps).setString(1, "Larry");
        verify(ps).setString(2, "correct-password");
    }

    @Test
    @DisplayName("login should not be bypassed by SQL injection payload in password")
    void loginShouldNotAllowSqlInjectionInPassword() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        // For an injection payload, the DB should behave as if no row matches
        when(rs.next()).thenReturn(false);

        String injectionPassword = "' OR '1'='1";

        // Act
        AttackResult result = assignment5.login("Larry", injectionPassword);

        // Assert
        assertEquals("failed", result.getLessonCompleted(), "Injection payload must not bypass authentication");

        // Ensure that the injection string is passed as a bound parameter, not spliced into SQL
        verify(ps).setString(1, "Larry");
        verify(ps).setString(2, injectionPassword);
    }
}
