package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
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

/**
 * Delta tests for Assignment5 focusing on the fixed SQL injection behavior:
 * - Verifies that the login method uses parameterized PreparedStatement with two parameters
 *   and does not depend on string-concatenated SQL.
 * - Verifies that valid credentials still succeed.
 *
 * Note: This is a pure unit test using mocks; it does not execute a real database.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login should prepare parameterized SQL and succeed for valid Larry credentials")
    void login_usesParameterizedQuery_andSucceedsForValidUser() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                eq("select password from challenge_users where userid = ? and password = ?")))
            .thenReturn(preparedStatement);
        // simulate one matching row
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "safePassword";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // verify parameter binding behavior: first parameter is username, second is password
        Mockito.verify(preparedStatement).setString(1, username);
        Mockito.verify(preparedStatement).setString(2, password);

        // validate that the result is the success path and still uses the same flag
        assertEquals("success", result.getLessonStatus().toString().toLowerCase());
        assertSame(assignment5, result.getLesson());
    }

    @Test
    @DisplayName("login should fail when username is not Larry (guards against bypass regardless of SQL changes)")
    void login_failsForNonLarryUser() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Bob", "anyPassword");

        // Assert
        assertEquals("failure", result.getLessonStatus().toString().toLowerCase());
        assertSame(assignment5, result.getLesson());
    }

    @Test
    @DisplayName("login should validate inputs and not hit DB when parameters are blank")
    void login_rejectsBlankCredentials_withoutDBCall() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login(" ", " ");

        // Assert
        // No DB interaction expected when validation fails early
        Mockito.verifyNoInteractions(dataSource);
        assertEquals("failure", result.getLessonStatus().toString().toLowerCase());
        assertSame(assignment5, result.getLesson());
    }
}
