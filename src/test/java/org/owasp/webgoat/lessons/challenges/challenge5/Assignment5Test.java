// Assuming package based on source file location.
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
 * Delta tests for Assignment5 focusing on the change from string-concatenated SQL
 * to a parameterized PreparedStatement. These tests verify that:
 * - The happy-path login still works when the correct credentials are provided.
 * - Injection-style credentials no longer cause unintended success.
 *
 * NOTE: We do NOT assert on SQL text directly; instead we rely on behavior:
 * the updated code uses parameters and should only succeed when the bound values match.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login should succeed for correct Larry credentials (behavior preserved with prepared statement)")
    void loginSucceedsWithCorrectCredentials() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);

        // Simulate a row found for the given parameters
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "correct-password");

        // Assert
        // If the prepared statement is used correctly with parameters, this path should succeed.
        assertEquals(true, result.isSuccessful(), "Expected login to succeed for valid Larry credentials");
    }

    @Test
    @DisplayName("login should NOT succeed when password contains SQL injection pattern")
    void loginFailsForSqlInjectionPassword() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);

        // For an injection attempt, the prepared statement should bind the exact string,
        // and the query should behave as if the data is not matching any row.
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String injectionPassword = "anything' OR '1'='1";

        // Act
        AttackResult result = assignment5.login("Larry", injectionPassword);

        // Assert
        // With the fix, this must NOT succeed anymore.
        assertEquals(false, result.isSuccessful(),
                "Login must fail when password contains SQL injection pattern even for Larry");
    }

    @Test
    @DisplayName("login should fail when username is not Larry (unchanged behavior)")
    void loginFailsForNonLarryUser() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Alice", "some-password");

        // Assert
        assertEquals(false, result.isSuccessful(),
                "Non-Larry usernames must still be rejected after the SQL fix");
    }
}
