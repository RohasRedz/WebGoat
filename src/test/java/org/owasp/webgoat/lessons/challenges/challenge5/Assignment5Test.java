package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Delta unit tests for Assignment5 focusing only on the changed behavior:
 * - Use of parameterized PreparedStatement instead of string concatenation.
 * - Ensuring login remains successful for valid credentials.
 */
class Assignment5Test {

    @Test
    @DisplayName("login uses PreparedStatement with bound parameters and succeeds for valid credentials")
    void login_usesPreparedStatementWithParameters_andSucceeds() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "securePassword");

        // Assert  verify query text is parameterized (no concatenated inputs)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL must use placeholders instead of concatenating user input"
        );

        // Assert  verify parameters are bound via setString (defense against SQL injection)
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "securePassword");

        // Assert  functional behavior preserved for valid credentials
        assertTrue(result.isLessonSolved(), "Login should succeed for valid credentials");

        // Also ensure no unexpected SQL containing raw user input was used
        assertTrue(
                !usedSql.contains("Larry") && !usedSql.contains("securePassword"),
                "SQL text itself must not contain raw user values"
        );
    }

    @Test
    @DisplayName("login still fails gracefully when no result is returned (behavior unchanged)")
    void login_failsWhenNoResult_found() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "wrongPassword");

        // Assert  behavior for invalid credentials remains the same
        assertEquals(false, result.isLessonSolved(), "Login should fail for invalid credentials");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "wrongPassword");
    }
}
