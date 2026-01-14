package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing only on the SQL injection fix:
 * - Verifies that the query uses parameter placeholders instead of string concatenation.
 * - Verifies that user-supplied username and password are bound via setString.
 *
 * Note: This is a behavior-focused test that asserts the interaction with JDBC
 * using Mockito mocks; it does not inspect private strings directly.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized PreparedStatement and binds user inputs correctly")
    void login_usesParameterizedQueryAndBindsParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);

        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("dummy-flag");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "P@ssw0rd!";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1) Verify PreparedStatement is created with a query that contains placeholders.
        Mockito.verify(connection).prepareStatement(
                Mockito.argThat(sql ->
                        sql != null
                                && sql.toLowerCase().contains("from challenge_users".toLowerCase())
                                && sql.contains("userid = ?")
                                && sql.contains("password = ?")
                                && !sql.contains(username)
                                && !sql.contains(password)
                )
        );

        // 2) Verify that user inputs are bound using setString in the correct order.
        InOrder inOrder = Mockito.inOrder(preparedStatement);
        inOrder.verify(preparedStatement).setString(1, username);
        inOrder.verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        // 3) Ensure successful path still works (challenge solved).
        org.junit.jupiter.api.Assertions.assertTrue(result.getLessonCompleted());
        org.junit.jupiter.api.Assertions.assertTrue(
                StringUtils.hasText(result.getOutput())
                        || StringUtils.hasText(result.getFeedback())
        );

        verifyNoMoreInteractions(preparedStatement);
    }

    @Test
    @DisplayName("login fails early when username or password is empty (guard condition preserved)")
    void login_rejectsEmptyUsernameOrPassword() throws Exception {
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult resultEmptyUser = assignment5.login("", "secret");
        AttackResult resultEmptyPassword = assignment5.login("Larry", " ");

        org.junit.jupiter.api.Assertions.assertFalse(resultEmptyUser.getLessonCompleted());
        org.junit.jupiter.api.Assertions.assertFalse(resultEmptyPassword.getLessonCompleted());

        // Ensure no DB interaction occurs when guard fails
        Mockito.verifyNoInteractions(dataSource);
    }

    @Test
    @DisplayName("login still restricts access to user Larry only (behavior unchanged)")
    void login_onlyAllowsLarryUser() throws Exception {
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("Mallory", "any");

        org.junit.jupiter.api.Assertions.assertFalse(result.getLessonCompleted());
        Mockito.verifyNoInteractions(dataSource);
    }
}
