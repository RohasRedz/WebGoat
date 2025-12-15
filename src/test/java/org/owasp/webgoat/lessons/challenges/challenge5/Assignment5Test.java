package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix:
 * - Verifies that user inputs are bound as PreparedStatement parameters
 *   instead of being concatenated into the SQL string.
 */
class Assignment5Test {

    @Test
    void loginShouldUsePreparedStatementParametersNotConcatenation() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);

        Assignment5 assignment = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "anyPassword";

        // Act
        AttackResult result = assignment.login(username, password);

        // Assert
        // Ensure that a parametrized query is used (with placeholders) – we cannot see the exact
        // SQL string here, but we can ensure that parameters are being set.
        verify(connection, times(1)).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(ps, times(1)).setString(1, username);
        verify(ps, times(1)).setString(2, password);

        // Check that the logical path (valid credentials) still succeeds.
        assertThat(result.getLessons()).isNotNull();
        assertThat(result.isLessonCompleted())
                .as("Challenge should still be solvable after the fix")
                .isTrue();
    }

    @Test
    void loginShouldFailForNonLarryUserEvenWithSqlInjectionPayload() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true); // Even if DB would return something

        Assignment5 assignment = new Assignment5(dataSource, flags);

        // Attack-style payload trying to bypass authentication
        String maliciousUsername = "Larry' OR '1'='1";
        String maliciousPassword = "anything";

        // Act
        AttackResult result = assignment.login(maliciousUsername, maliciousPassword);

        // Assert
        // Business rule: only exact "Larry" is allowed.
        assertThat(result.isLessonCompleted())
                .as("SQL injection payload must not bypass the user equality check")
                .isFalse();

        // Still ensure parameters are set to the raw input (no concatenation).
        verify(connection, times(0)).prepareStatement(contains(maliciousUsername));
        verify(ps, times(1)).setString(1, maliciousUsername);
        verify(ps, times(1)).setString(2, maliciousPassword);
    }
}
