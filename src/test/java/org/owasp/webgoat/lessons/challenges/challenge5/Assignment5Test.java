// Assuming standard package based on source path; adjust if actual package differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix.
 *
 * Before fix:
 *   PreparedStatement statement =
 *       connection.prepareStatement(
 *           "select password from challenge_users where userid = '"
 *               + username_login
 *               + "' and password = '"
 *               + password_login
 *               + "'"
 *
 * After fix:
 *   PreparedStatement statement =
 *       connection.prepareStatement(
 *           "select password from challenge_users where userid = ? and password = ?");
 *   statement.setString(1, username_login);
 *   statement.setString(2, password_login);
 *
 * This test verifies:
 * - The login method uses parameterized SQL and binds user input into parameters.
 * - A typical successful login scenario still works (behavior preserved).
 */
public class Assignment5Test {

    @Nested
    @DisplayName("SQL injection fix behavior")
    class SqlInjectionFixTests {

        @Test
        @DisplayName("login() should use prepared statement parameters for username and password")
        void login_usesPreparedStatementParameters() throws Exception {
            // Arrange
            LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
            Flags flags = Mockito.mock(Flags.class);
            Assignment5 assignment5 = new Assignment5(dataSource, flags);

            Connection connection = Mockito.mock(Connection.class);
            PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
            ResultSet resultSet = Mockito.mock(ResultSet.class);

            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(
                    "select password from challenge_users where userid = ? and password = ?"))
                    .thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(flags.getFlag(5)).thenReturn("dummy-flag");

            String username = "Larry";
            String password = "anyPassword123";

            // Act
            AttackResult result = assignment5.login(username, password);

            // Assert
            // Verify parameters are bound in order, instead of being concatenated in SQL.
            Mockito.verify(preparedStatement).setString(1, username);
            Mockito.verify(preparedStatement).setString(2, password);
            Mockito.verify(preparedStatement).executeQuery();

            assertTrue(result.getOutput().contains("challenge.solved"),
                    "Expected successful challenge feedback when credentials are valid");
        }

        @Test
        @DisplayName("login() should still fail when result set is empty (behavior preserved)")
        void login_failsWhenNoResult() throws Exception {
            // Arrange
            LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
            Flags flags = Mockito.mock(Flags.class);
            Assignment5 assignment5 = new Assignment5(dataSource, flags);

            Connection connection = Mockito.mock(Connection.class);
            PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
            ResultSet resultSet = Mockito.mock(ResultSet.class);

            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(
                    "select password from challenge_users where userid = ? and password = ?"))
                    .thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            String username = "Larry";
            String password = "wrongPassword";

            // Act
            AttackResult result = assignment5.login(username, password);

            // Assert
            Mockito.verify(preparedStatement).setString(1, username);
            Mockito.verify(preparedStatement).setString(2, password);
            assertTrue(result.getOutput().contains("challenge.close"),
                    "Expected failure feedback when credentials are invalid");
        }

        @Test
        @DisplayName("login() should reject non-Larry usernames before hitting SQL (defense in depth)")
        void login_rejectsNonLarryBeforeQuery() throws Exception {
            // Arrange
            LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
            Flags flags = Mockito.mock(Flags.class);
            Assignment5 assignment5 = new Assignment5(dataSource, flags);

            String username = "Mallory";
            String password = "anything";

            // Act
            AttackResult result = assignment5.login(username, password);

            // Assert
            // Ensure that no DB interaction occurs for non-Larry usernames
            Mockito.verifyNoInteractions(dataSource);
            assertTrue(result.getOutput().contains("user.not.larry"),
                    "Expected early failure for non-Larry usernames");
        }
    }
}
