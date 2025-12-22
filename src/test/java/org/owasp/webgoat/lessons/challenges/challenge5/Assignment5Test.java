package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.*;
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
 * Delta tests focusing on the SQL injection fix:
 * - Verifies that Assignment5.login() uses a parameterized PreparedStatement
 *   with placeholders instead of string concatenation.
 * - Ensures that user-supplied username and password are bound via setString.
 */
class Assignment5Test {

    @Test
    @DisplayName("login() should use parameterized query and bind username and password")
    void loginUsesPreparedStatementWithParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("dummy-flag");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // simulate successful login

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "SecurePassword1!";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertNotNull(result, "AttackResult should not be null after login");

        // Verify prepared statement SQL uses placeholders instead of concatenation
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertTrue(
                usedSql.toLowerCase().contains("where userid = ? and password = ?"),
                "SQL must use parameter placeholders for userid and password");

        // Verify the exact parameter values passed as bound parameters, not concatenated into SQL
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Also ensure no alternative prepareStatement call with concatenated user input occurred
        verify(connection, times(1)).prepareStatement(anyString());
    }
}
