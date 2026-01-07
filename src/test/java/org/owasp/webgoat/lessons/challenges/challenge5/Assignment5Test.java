// Delta test for BATCH-001: Assignment5.java
// File path (inferred): src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java

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
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

// NOTE: This is a delta test focusing only on the changed behavior:
// - The SQL query must use parameter placeholders and bind user inputs via setString
// - No direct string concatenation of user input into the SQL string.

class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized PreparedStatement and binds username and password correctly")
    void login_usesParameterizedQuery_andBindsParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("FLAG5");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "safePassword123";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());

        String usedSql = sqlCaptor.getValue();
        // The SQL must contain parameter placeholders and not raw user input.
        assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL should use parameter placeholders for userid and password");
        assertTrue(
                !usedSql.contains(username) && !usedSql.contains(password),
                "SQL must not contain raw user input values");

        // Verify correct parameter binding order and values
        verify(preparedStatement).setString(eq(1), eq(username));
        verify(preparedStatement).setString(eq(2), eq(password));

        // Ensure behavior for valid credentials still indicates success
        assertTrue(result.getLessonCompleted(), "Login with valid credentials should still succeed");
        assertEquals("FLAG5", result.getFeedbackArgs()[0], "Flag should be returned on success");
    }
}
