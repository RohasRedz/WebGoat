// Assuming standard Maven-style test package based on source path
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.owasp.webgoat.container.assignments.AttackResult.AttackResultType.SUCCESS;
import static org.owasp.webgoat.container.assignments.AttackResult.AttackResultType.FAIL;

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
 * Delta tests for Assignment5 focusing only on:
 * - SQL query now uses parameterized PreparedStatement instead of string concatenation.
 */
class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized PreparedStatement and bind user inputs safely")
    void loginUsesParameterizedQuery() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry";
        String password = "p@ssw0rd";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1. Ensure query text uses placeholders instead of concatenated values
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("userid = ?").contains("password = ?");
        assertThat(sql).doesNotContain(username).doesNotContain(password);

        // 2. Ensure parameters are bound correctly without concatenation
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // 3. Ensure the path still works and returns success in the positive case
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(SUCCESS);
    }

    @Test
    @DisplayName("login should not succeed when credentials do not match even with parameterization")
    void loginWithInvalidCredentialsShouldFailUsingSameParameterizedQuery() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No rows -> invalid credentials

        String username = "Larry";
        String password = "wrong";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(FAIL);
    }
}
