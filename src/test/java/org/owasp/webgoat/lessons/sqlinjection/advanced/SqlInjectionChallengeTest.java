package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionChallenge.
 *
 * Focus: changed behavior in the user-existence check query:
 * - Must use a parameterized PreparedStatement with a single "?" placeholder.
 * - Must bind the username via setString(1, username).
 * - Must NOT concatenate username into the SQL string.
 *
 * These tests use mocks to avoid any real database interaction.
 */
public class SqlInjectionChallengeTest {

    @Test
    void registerNewUser_shouldUseParameterizedQueryForUserExistenceCheck() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement checkUserPs = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        PreparedStatement insertPs = mock(PreparedStatement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString()))
                .thenReturn(checkUserPs)
                .thenReturn(insertPs); // second call for insert
        when(checkUserPs.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // user does not exist, so we go to insert

        SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

        String username = "victim";
        String email = "victim@example.com";
        String password = "s3cret!";

        // Act
        AttackResult result = challenge.registerNewUser(username, email, password);

        // Assert: check SELECT query string and bound parameter
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection, atLeastOnce()).prepareStatement(sqlCaptor.capture());

        String selectSql = sqlCaptor.getAllValues().get(0);
        assertThat(selectSql.toLowerCase())
                .contains("select userid from sql_challenge_users where userid = ?");

        // Ensure raw username is not concatenated into the SQL string
        assertThat(selectSql).doesNotContain(username);

        // Ensure username is bound via parameter
        verify(checkUserPs).setString(eq(1), eq(username));

        // Also assert that the overall flow still succeeds (user created)
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse(); // info message, not completion
        verify(insertPs).setString(1, username);
        verify(insertPs).setString(2, email);
        verify(insertPs).setString(3, password);
        verify(insertPs).execute();
    }

    @Test
    void registerNewUser_shouldHandleExistingUserWithoutConcatenatingUsername() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement checkUserPs = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(checkUserPs);
        when(checkUserPs.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // user already exists

        SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

        String username = "existingUser";
        String email = "ignored@example.com";
        String password = "ignored";

        // Act
        AttackResult result = challenge.registerNewUser(username, email, password);

        // Assert: query is parameterized and username bound
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());

        String selectSql = sqlCaptor.getValue();
        assertThat(selectSql.toLowerCase())
                .contains("select userid from sql_challenge_users where userid = ?");

        assertThat(selectSql).doesNotContain(username);
        verify(checkUserPs).setString(eq(1), eq(username));

        // No insert should occur when user exists
        verify(connection, times(1)).prepareStatement(anyString());
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }

    @Test
    void registerNewUser_shouldNotConstructSqlWhenArgumentsInvalid() throws SQLException {
        // This ensures the new parameterized logic is not invoked when validation fails,
        // preserving the prior behavior while still preventing injection.

        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

        // Act
        AttackResult result = challenge.registerNewUser("", "e@example.com", "pass");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
        verifyNoInteractions(dataSource);
    }
}
