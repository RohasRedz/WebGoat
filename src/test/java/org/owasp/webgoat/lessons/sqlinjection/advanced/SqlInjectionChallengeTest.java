package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionChallenge focusing on the parameterized checkUserQuery.
 *
 * These tests verify that:
 * - Injection payload in username does not break logic or bypass existence check.
 * - Normal registration flow still behaves as expected.
 */
class SqlInjectionChallengeTest {

    @Test
    @DisplayName("registerNewUser should not allow SQL injection in username to bypass checks")
    void registerNewUserShouldNotAllowSqlInjectionInUsername() throws SQLException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

        Connection conn = mock(Connection.class);
        PreparedStatement checkStmt = mock(PreparedStatement.class);
        ResultSet checkRs = mock(ResultSet.class);
        PreparedStatement insertStmt = mock(PreparedStatement.class);

        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString()))
                .thenReturn(checkStmt)   // first call for checkUserQuery
                .thenReturn(insertStmt); // second call for insert
        when(checkStmt.executeQuery()).thenReturn(checkRs);
        when(checkRs.next()).thenReturn(false); // simulate "user does not exist" even for injection
        when(insertStmt.execute()).thenReturn(true);

        String injectionUsername = "bob' OR '1'='1";
        String email = "bob@example.com";
        String password = "secret";

        // Act
        AttackResult result = challenge.registerNewUser(injectionUsername, email, password);

        // Assert
        // With parameterized query, the injection username is treated as literal and does not break logic.
        assertEquals("info", result.getLessonCompleted(),
                "User creation path should operate normally, but without any injection-based bypass");

        // Verify that the checkUserQuery used a placeholder and bound username as a parameter
        verify(checkStmt).setString(1, injectionUsername);
    }

    @Test
    @DisplayName("registerNewUser should succeed for normal valid registration data")
    void registerNewUserShouldSucceedForValidData() throws SQLException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

        Connection conn = mock(Connection.class);
        PreparedStatement checkStmt = mock(PreparedStatement.class);
        ResultSet checkRs = mock(ResultSet.class);
        PreparedStatement insertStmt = mock(PreparedStatement.class);

        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString()))
                .thenReturn(checkStmt)
                .thenReturn(insertStmt);
        when(checkStmt.executeQuery()).thenReturn(checkRs);
        when(checkRs.next()).thenReturn(false); // user does not exist yet
        when(insertStmt.execute()).thenReturn(true);

        String username = "alice";
        String email = "alice@example.com";
        String password = "password123";

        // Act
        AttackResult result = challenge.registerNewUser(username, email, password);

        // Assert
        assertEquals("info", result.getLessonCompleted(), "Normal registration should succeed");

        // Ensure parameters are bound for both the check and the insert
        verify(checkStmt).setString(1, username);
        verify(insertStmt).setString(1, username);
        verify(insertStmt).setString(2, email);
        verify(insertStmt).setString(3, password);
    }
}
