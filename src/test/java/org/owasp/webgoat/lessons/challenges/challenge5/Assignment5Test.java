package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
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
 * Delta tests focused on the vulnerability fix:
 * - Ensure that Assignment5.login(...) uses a parameterized PreparedStatement
 *   and no longer concatenates user input into the SQL query string.
 */
class Assignment5Test {

    @Test
    @DisplayName("login should use PreparedStatement with parameter placeholders and bound parameters")
    void login_usesPreparedStatementWithParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("flag-5");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "SecurePass123";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1. SQL must use parameter placeholders instead of concatenation
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        // The secured SQL should contain placeholders and not the raw values
        // This asserts the fix (parameterized query) is in place.
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL must use '?' placeholders for user parameters"
        );
        org.junit.jupiter.api.Assertions.assertFalse(
                usedSql.contains(username) || usedSql.contains(password),
                "SQL must not contain raw user input values directly concatenated"
        );

        // 2. Parameters must be bound via setString
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // 3. Behavior is preserved for valid credentials
        assertEquals("success", result.getLessonPhase(), "Expected successful attack result for valid credentials");
    }
}
