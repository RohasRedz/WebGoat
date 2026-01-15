package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
 * Delta tests focusing only on the changed behavior in Assignment5:
 * - SQL query must use parameter placeholders instead of string concatenation
 * - User-supplied values must be bound via PreparedStatement.setString(...)
 */
class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized query and bind username and password via setString")
    void login_usesParameterizedQueryAndBindsParameters() throws Exception {
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
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert - functional success unchanged
        assertSame(AttackResult.Status.SUCCESS, result.getLessonStatus());

        // Assert - statement text must contain placeholders, not inlined user values
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        // Ensure parameter markers are used
        // (we care only about presence of ? placeholders, not exact casing/spacing)
        org.junit.jupiter.api.Assertions.assertTrue(
                sql.contains("userid = ?") && sql.contains("password = ?"),
                "SQL should use parameter placeholders for userid and password");

        // Ensure no direct concatenation of user input into SQL
        org.junit.jupiter.api.Assertions.assertFalse(
                sql.contains(username) || sql.contains(password),
                "SQL string must not contain raw user values");

        // Assert - parameters are bound in proper order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    @DisplayName("login should still fail for non-Larry user, preserving original behavior")
    void login_nonLarryUserStillFails() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Mallory", "doesnt-matter");

        // Assert - behavior: still fails and does not reach SQL execution
        assertSame(AttackResult.Status.FAIL, result.getLessonStatus());
        verifyNoInteractions(dataSource);
    }
}
