// TODO: Package name inferred from the updated source; adjust if the real package differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Delta unit tests for Assignment5.
 *
 * Vulnerability description:
 * - SQL query was previously built via string concatenation with user input, enabling SQL injection.
 *
 * Changed behavior verified here:
 * - SQL is now parameterized (uses '?' placeholders).
 * - User-supplied username and password are bound via PreparedStatement#setString instead of inlined.
 */
public class Assignment5Test {

    private LessonDataSource mockLessonDataSource(Connection connection) throws Exception {
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        when(lessonDataSource.getConnection()).thenReturn(connection);
        return lessonDataSource;
    }

    @Test
    @DisplayName("login uses parameterized SQL and does not inline user input into the query")
    void login_usesParameterizedSql_andBindsUserInput() throws Exception {
        // Arrange
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // force failure to avoid unrelated branches

        LessonDataSource lessonDataSource = mockLessonDataSource(connection);
        Flags flags = mock(Flags.class);
        Assignment5 endpoint = new Assignment5(lessonDataSource, flags);

        String username = "Larry";
        String password = "any' OR '1'='1"; // injection-like payload

        // Act
        AttackResult result = endpoint.login(username, password);

        // Assert: SQL must use placeholders and not contain raw input
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sqlUsed = sqlCaptor.getValue();

        assertTrue(
                sqlUsed.contains("where userid = ? and password = ?"),
                "SQL must use placeholders for userid and password");
        assertFalse(
                sqlUsed.contains(username) || sqlUsed.contains(password),
                "User-controlled values must not be concatenated into the SQL string");

        // Assert: parameters must be bound in correct order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Behavior: login still fails for incorrect password (unchanged functional outcome)
        assertTrue(result.isFailed());
    }
}
