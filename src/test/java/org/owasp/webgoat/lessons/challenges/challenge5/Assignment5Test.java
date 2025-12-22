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
 * Delta test for Assignment5 focusing only on the SQL-injection fix:
 * - verifies parameterized query is used (placeholders + setString),
 *   instead of direct string concatenation with user input.
 */
class Assignment5Test {

    @Test
    @DisplayName("login() should use PreparedStatement with parameter placeholders and setString bindings")
    void login_usesPreparedStatementParameters() throws Exception {
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
        when(flags.getFlag(5)).thenReturn("flag-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert basic success to ensure the happy path still works
        assertTrue(result.getSuccess(), "Expected successful attack result when credentials match");

        // Capture the SQL and verify it uses placeholders instead of inline user values
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        // The fixed code should have parameter placeholders
        assertTrue(
                usedSql.contains("where userid = ?") && usedSql.contains("password = ?"),
                "SQL should use parameter placeholders instead of concatenating user input"
        );

        // Verify that username and password are bound via setString in correct order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Ensure no direct concatenation pattern is present (defense-in-depth assertion)
        assertEquals(
                -1,
                usedSql.indexOf(username),
                "Username should not be concatenated directly into the SQL string"
        );
        assertEquals(
                -1,
                usedSql.indexOf(password),
                "Password should not be concatenated directly into the SQL string"
        );
    }
}
