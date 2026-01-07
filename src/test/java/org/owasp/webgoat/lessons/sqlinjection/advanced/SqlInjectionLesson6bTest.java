package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests focused on:
 * - getPassword() no longer using a hardcoded fallback password.
 * - getPassword() using PreparedStatement with parameter binding.
 * - completed(...) behavior when no password is available vs. correct password.
 */
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword uses PreparedStatement with parameter binding for user 'dave'")
    void getPassword_usesPreparedStatementAndBindsUsername() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(dataSource);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Simulate that password is present in DB
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secretFromDb");

        // Act
        String password = endpoint.getPassword();

        // Assert
        // Verify secure SQL and parameter binding
        verify(connection).prepareStatement("SELECT password FROM user_system_data WHERE user_name = ?");
        verify(preparedStatement).setString(1, "dave");
        verify(preparedStatement).executeQuery();

        // Ensure value comes from DB, not from any hardcoded default
        assertThat(password).isEqualTo("secretFromDb");
    }

    @Test
    @DisplayName("getPassword returns null when no DB row is found (no hardcoded fallback)")
    void getPassword_returnsNullWhenNoRowFound() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(dataSource);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // No row found
        when(resultSet.first()).thenReturn(false);

        // Act
        String password = endpoint.getPassword();

        // Assert
        // Previously it would have returned hardcoded "dave"; now it must be null
        assertThat(password).as("No row found should not yield a hardcoded fallback password").isNull();
    }

    @Test
    @DisplayName("completed fails when userid_6b does not match DB password (no fallback bypass)")
    void completed_failsWhenUseridDoesNotMatchPasswordOrPasswordMissing() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b endpointSpy = spy(new SqlInjectionLesson6b(dataSource));

        // Simulate missing password (e.g., DB error or no result)
        doReturn(null).when(endpointSpy).getPassword();

        // Act
        AttackResult result = endpointSpy.completed("dave");

        // Assert
        // With null password, equality should fail and AttackResult should not mark lesson as completed
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).as("No password available must not pass the challenge").isFalse();
    }

    @Test
    @DisplayName("completed succeeds only when userid_6b equals the retrieved password")
    void completed_succeedsOnlyWhenUseridMatchesPassword() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b endpointSpy = spy(new SqlInjectionLesson6b(dataSource));

        // Simulate a password retrieved from DB
        doReturn("secretFromDb").when(endpointSpy).getPassword();

        // Act
        AttackResult success = endpointSpy.completed("secretFromDb");
        AttackResult failure = endpointSpy.completed("wrongUser");

        // Assert
        assertThat(success.getLessonCompleted()).isTrue();
        assertThat(failure.getLessonCompleted()).isFalse();
    }
}
