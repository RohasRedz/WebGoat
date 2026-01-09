package org.owasp.webgoat.lessons.challenges.challenge5;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Delta tests for Assignment5 focusing on the vulnerability fix:
 * using PreparedStatement (parameterized query) instead of string concatenation.
 */
class Assignment5Test {

    @Test
    void getUserDataUsesParameterizedQueryAndBindsUsername() throws SQLException {
        // Arrange
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        String username = "victimUser";
        String expectedData = "secret-data";

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("data")).thenReturn(expectedData);

        Assignment5 assignment5 = new Assignment5();

        // Act
        String result = assignment5.getUserData(connection, username);

        // Assert - returned data
        assertEquals(expectedData, result, "Should return data from the result set");

        // Assert - query text expected to use a placeholder (no direct concatenation)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertTrue(usedSql.contains("WHERE username = ?"),
                "SQL must use a parameter placeholder instead of concatenating the username");

        // Assert - parameter binding uses index 1 with the provided username
        verify(preparedStatement).setString(1, username);

        // Ensure no direct executeQuery(String) on Statement-like API is used
        verify(preparedStatement, times(1)).executeQuery();
    }

    @Test
    void getUserDataReturnsNullWhenNoRowFound() throws SQLException {
        // Arrange
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5();

        // Act
        String result = assignment5.getUserData(connection, "nonExistingUser");

        // Assert
        assertNull(result, "Should return null when no record is found");
    }
}
