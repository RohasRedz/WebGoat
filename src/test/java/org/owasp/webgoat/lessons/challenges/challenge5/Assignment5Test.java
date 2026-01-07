// Test file path (mirrors main with 'main' -> 'test'):
// src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * - login() must use parameterized SQL and behave correctly for:
 *   - valid credentials
 *   - invalid credentials
 *   - SQL injection-style payloads (should NOT succeed)
 *
 * These tests do not assert SQL construction directly but verify
 * observable behavior that the injection vector no longer works.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login with valid Larry credentials succeeds (parameterized query still works)")
    void login_withValidLarryCredentials_succeeds() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 endpoint = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = endpoint.login("Larry", "password123");

        // Assert
        assertEquals(true, result.isSuccess(), "Expected login to succeed for valid Larry credentials");
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(ps).setString(1, "Larry");
        verify(ps).setString(2, "password123");
    }

    @Test
    @DisplayName("login with SQL injection payload does not bypass authentication")
    void login_withSqlInjectionPayload_fails() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        // Simulate no row returned even for injection payload because parameters are used
        when(rs.next()).thenReturn(false);

        Assignment5 endpoint = new Assignment5(dataSource, flags);

        String maliciousPassword = "' OR '1'='1";

        // Act
        AttackResult result = endpoint.login("Larry", maliciousPassword);

        // Assert
        assertEquals(false, result.isSuccess(), "SQL injection payload must not bypass authentication");
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(ps).setString(1, "Larry");
        verify(ps).setString(2, maliciousPassword);
    }

    @Test
    @DisplayName("login with non-Larry username is rejected before hitting database")
    void login_withNonLarryUser_failsWithoutDbCall() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Assignment5 endpoint = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = endpoint.login("Bob", "anyPassword");

        // Assert
        assertEquals(false, result.isSuccess(), "Non-Larry users must be rejected");
        // Ensure no DB interaction takes place (unchanged but important to assert after refactor)
        verifyNoInteractions(dataSource);
    }
}
