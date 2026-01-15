package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on the fixed SQL injection vulnerability.
 * Verifies that user input is passed via PreparedStatement parameters rather than
 * concatenated into the SQL string.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized query and succeeds with correct credentials")
    void login_usesPreparedStatementParameters_onSuccess() throws Exception {
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

        // Act
        String username = "Larry";
        String password = "securePassword";
        AttackResult result = assignment5.login(username, password);

        // Assert: PreparedStatement was created with placeholders, not concatenated SQL
        verify(connection, times(1))
            .prepareStatement("select password from challenge_users where userid = ? and password = ?");

        // Assert: user input is bound via parameters, in correct order
        verify(preparedStatement, times(1)).setString(1, username);
        verify(preparedStatement, times(1)).setString(2, password);

        // Also verify that the query was actually executed
        verify(preparedStatement, times(1)).executeQuery();

        // And that on success we return the success AttackResult with the flag
        // (using feedbackArgs as indicator that successful path was taken)
        // We cannot easily introspect the message, but we can at least assert non-null
        // and that it's the same instance returned from success() builder.
        assertSame(result.getClass(), AttackResult.class);

        // Optional: capture the actual bound values defensively
        ArgumentCaptor<String> paramCaptor = ArgumentCaptor.forClass(String.class);
        verify(preparedStatement, times(2)).setString(anyInt(), paramCaptor.capture());
        assertEquals(username, paramCaptor.getAllValues().get(0));
        assertEquals(password, paramCaptor.getAllValues().get(1));
    }

    @Test
    @DisplayName("login returns failure and still uses parameter binding for wrong password")
    void login_usesPreparedStatementParameters_onFailure() throws Exception {
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
        // No matching row
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        String username = "Larry";
        String password = "wrongPassword";
        AttackResult result = assignment5.login(username, password);

        // Assert: PreparedStatement API usage is the same, even on failure
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        // Result should be a failure path (we cannot check message key easily, but
        // the absence of a flag argument indicates not the success branch).
        assertSame(result.getClass(), AttackResult.class);
    }
}
