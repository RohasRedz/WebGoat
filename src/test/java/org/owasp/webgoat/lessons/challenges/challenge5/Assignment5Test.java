package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests focusing on the SQL injection fix in Assignment5.login().
 *
 * These tests verify:
 * - A parameterized query is used (no SQL concatenation with user input).
 * - Login succeeds for a normal user/password.
 * - Login fails for an injection-like password instead of being treated as a valid login.
 *
 * NOTE: We cannot directly assert the SQL string content without refactoring the class,
 * so we assert correct outcome behavior and parameter binding interactions.
 */
class Assignment5Test {

    @Test
    @DisplayName("login succeeds for valid Larry credentials")
    void loginSucceedsForValidLarryCredentials() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "correct-password");

        // Assert
        // verify parameterized query usage via parameter binding interactions
        verify(connection).prepareStatement(anyString());
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "correct-password");
        verify(preparedStatement).executeQuery();
        assertEquals(true, result.getLessonCompleted(), "Expected challenge to be solved for valid Larry credentials");
    }

    @Test
    @DisplayName("login fails for SQL-injection-like password")
    void loginFailsForSqlInjectionLikePassword() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Simulate no row returned for injection attempt
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String injectionPassword = "' OR '1'='1";

        // Act
        AttackResult result = assignment5.login("Larry", injectionPassword);

        // Assert
        // parameters must be bound exactly as provided (no query manipulation)
        verify(connection).prepareStatement(anyString());
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, injectionPassword);
        verify(preparedStatement).executeQuery();
        assertEquals(false, result.getLessonCompleted(), "Injection-like password must not bypass authentication");
    }
}
