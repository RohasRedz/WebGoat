package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
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
 * Delta unit tests for Assignment5 focusing only on:
 * - Use of parameterized PreparedStatement instead of string concatenation for SQL
 * - Ensuring user inputs are bound via setString(…) and not interpolated into the SQL text
 */
public class Assignment5Test {

    @Test
    @DisplayName("login should use a parameterized PreparedStatement instead of string concatenation")
    void login_usesParameterizedPreparedStatement() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "p@ssw0rd";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Ensure the query text is the parameterized version
        verify(connection).prepareStatement(eq(
                "select password from challenge_users where userid = ? and password = ?"));

        // Verify that user inputs are bound as parameters, not concatenated into SQL
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        verify(preparedStatement).executeQuery();
        assertTrue(result.getLessonCompleted(),
                "Successful login should still mark the challenge as solved after the SQLi fix");
        assertTrue(result.getFeedback().contains("FLAG-5"),
                "Successful login should still return the correct flag after the SQLi fix");
    }

    @Test
    @DisplayName("login should not execute query when username is not 'Larry'")
    void login_doesNotExecuteQueryForNonLarryUser() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Bob", "anything");

        // Assert
        // Ensure no SQL is executed for invalid user to validate secure control flow is unchanged
        verify(connection, never()).prepareStatement(anyString());
        assertEquals(false, result.getLessonCompleted(),
                "Non-Larry user should not succeed even after SQLi fix");
    }
}
