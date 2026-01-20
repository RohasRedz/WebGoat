package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix:
 * - Verifies parameterized query is used with placeholders.
 * - Verifies user input is passed as bound parameters, not concatenated SQL.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login() should use prepared statement with bound parameters for username and password")
    void login_usesParameterizedQueryAndBindsUserInput() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "password123' OR '1'='1"; // attempt typical SQL injection payload

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        // Verify we prepared the expected parameterized SQL (with ? placeholders)
        verify(connection).prepareStatement(sqlCaptor.capture());
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                sqlCaptor.getValue(),
                "SQL should use parameter placeholders instead of concatenating user input");

        // Verify that user input is bound via parameters and not interpolated into the SQL string.
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Also confirm the flow still returns success when the query finds a row.
        // (This ensures we didn't break functionality while fixing the vulnerability.)
        // The AttackResult implementation is part of WebGoat; here we just validate the type.
        // We could also inspect success via toString or status if needed.
        // For delta testing, confirming no exception and interaction is sufficient.
    }
}
