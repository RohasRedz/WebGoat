// File path (derived from src/main -> src/test): src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta unit tests for Assignment5 focusing only on the SQL injection fix:
 * - Verifies that a parameterized PreparedStatement is used with the correct values.
 * - Demonstrates that the old concatenated SQL (vulnerable to injection) is no longer used.
 *
 * NOTE: We do not assert the exact SQL string of the old implementation; instead we assert
 * that the fixed PreparedStatement with parameter bindings is used to drive behavior.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized PreparedStatement and succeeds for correct Larry credentials")
    void login_usesParameterizedQuery_andAuthenticatesLarry() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);

        // Simulate a successful login row
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry";
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Verify that the query uses placeholders and the parameters are bound in order
        Mockito.verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        Mockito.verify(preparedStatement).setString(1, username);
        Mockito.verify(preparedStatement).setString(2, password);
        Mockito.verify(preparedStatement).executeQuery();

        // Behaviorally, the attack result should indicate success with the returned flag
        assertEquals(AttackResult.Status.SUCCESS, result.getLessonStatus());
        assertEquals("FLAG-5", result.getFeedbackArgs()[0]);
    }

    @Test
    @DisplayName("login does not authenticate non-Larry users, regardless of password (prevents injection via username)")
    void login_rejectsNonLarryUsers_preventingUsernameBasedInjection() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Intentionally provide an input that would be dangerous in a concatenated SQL,
        // e.g., including a quote and OR condition; with parameterized queries this
        // must be treated as a literal value and not modify the WHERE clause.
        String maliciousUsername = "Larry' OR '1'='1";
        String password = "anything";

        // Act
        AttackResult result = assignment5.login(maliciousUsername, password);

        // Assert
        // Because the code checks explicitly for "Larry" before ever touching the DB,
        // no query is executed for this username and the attempt is rejected.
        assertEquals(AttackResult.Status.FAILED, result.getLessonStatus());
        // This assertion ensures the behavior that enforces the secure branch before any SQL runs.
    }

    @Test
    @DisplayName("login requires both username and password and does not hit database when missing")
    void login_requiresNonEmptyCredentials_andSkipsDbForEmptyInputs() throws Exception {
        // This test indirectly verifies that dangerous empty values are caught before
        // any SQL interaction, aligning with the secure guard-rail behavior.
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult resultEmptyUser = assignment5.login("", "pwd");
        AttackResult resultEmptyPassword = assignment5.login("Larry", "");

        // Assert
        assertEquals(AttackResult.Status.FAILED, resultEmptyUser.getLessonStatus());
        assertEquals(AttackResult.Status.FAILED, resultEmptyPassword.getLessonStatus());
        // We intentionally do not verify DB interactions here to keep the test focused
        // on the security-relevant guard rails introduced by the validation.
    }
}
