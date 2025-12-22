// Assuming standard Maven/Gradle test source layout and matching package.
// File path: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
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
 * Delta tests for Assignment5 focusing only on the SQL injection fix:
 * - Verifies that a parameterized PreparedStatement is used.
 * - Verifies that user input is bound via setString and not concatenated into the SQL.
 */
class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized query and bind username and password as parameters")
    void login_usesParameterizedQuery() throws Exception {
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

        String username = "Larry";
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1) SQL text must contain placeholders instead of concatenated input
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        // The fixed code uses '? and ?' instead of embedding the username/password directly
        // We assert this without depending on exact whitespace formatting.
        // This would have failed before the fix because the SQL contained the raw username/password.
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.toLowerCase().contains("userid = ?") &&
                usedSql.toLowerCase().contains("password = ?"),
                "SQL must use parameter placeholders for userid and password");

        // 2) Parameters must be bound in order and with user-controlled values
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // 3) Behavior still succeeds when credentials are correct (regression guard)
        org.junit.jupiter.api.Assertions.assertTrue(result.getLessonCompleted(),
                "Lesson should be marked as completed when valid credentials are provided");
    }

    @Test
    @DisplayName("login should reject non-Larry usernames before SQL execution (unchanged behavior guard)")
    void login_rejectsNonLarryBeforeQueryExecution() throws Exception {
        // This acts as a guard to ensure the SQL query is not even prepared when username is not 'Larry'.
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Mallory", "anything");

        // Assert
        // ensure no SQL interaction happened for non-Larry user
        verify(dataSource, never()).getConnection();
        assertEquals(false, result.getLessonCompleted(),
                "Non-Larry user should not cause lesson completion");
    }
}
