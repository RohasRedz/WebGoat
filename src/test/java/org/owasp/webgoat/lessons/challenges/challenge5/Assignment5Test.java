package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Delta unit tests for {@link Assignment5}.
 *
 * These tests focus ONLY on the behavior changed to fix the SQL Injection vulnerability:
 * - The SQL query must be parameterized (using '?' placeholders).
 * - User input for username_login and password_login must be bound via setString,
 *   rather than concatenated into the SQL string.
 */
public class Assignment5Test {

    /**
     * Verifies that the login method builds a parameterized PreparedStatement and does not
     * concatenate username_login and password_login directly into the SQL query string.
     *
     * This test mocks the JDBC stack (LessonDataSource -> Connection -> PreparedStatement -> ResultSet)
     * and inspects the SQL and bound parameters.
     */
    @Test
    @DisplayName("login() should use parameterized SQL and bind username and password via setString")
    void loginUsesParameterizedPreparedStatement() throws Exception {
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
        String password = "p@ssw0rd";

        // Capture the SQL string used in prepareStatement
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert behavior of the method remains successful for correct credentials
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isTrue();

        // Verify that a PreparedStatement was created with a parameterized query
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        // The fixed code should use '?' placeholders instead of concatenating user input
        assertThat(usedSql)
                .as("SQL should be parameterized using '?' placeholders, not string concatenation of user input")
                .contains("userid = ?")
                .contains("password = ?");

        // Ensure raw user input values do not appear directly embedded in the SQL string
        assertThat(usedSql).doesNotContain(username);
        assertThat(usedSql).doesNotContain(password);

        // Verify that username and password are bound via setString on the PreparedStatement
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Ensure executeQuery is still called once
        verify(preparedStatement).executeQuery();
    }

    /**
     * Negative-path delta test: ensure that when an incorrect password is provided,
     * the query is still parameterized and the behavior is a clean failure.
     *
     * The focus remains on confirming that parameters are bound into the statement
     * and not concatenated into the SQL string.
     */
    @Test
    @DisplayName("login() with wrong password should still use parameterized SQL and fail cleanly")
    void loginWithWrongPasswordStillUsesParameterizedSql() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Simulate no matching row for wrong password
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String wrongPassword = "wrong";

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        AttackResult result = assignment5.login(username, wrongPassword);

        // Assert functional outcome
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();

        // Verify the SQL is parameterized even for failing credentials
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        assertThat(usedSql).contains("userid = ?").contains("password = ?");
        assertThat(usedSql).doesNotContain(username);
        assertThat(usedSql).doesNotContain(wrongPassword);

        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, wrongPassword);
        verify(preparedStatement).executeQuery();
    }
}
