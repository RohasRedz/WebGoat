package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta unit tests for Assignment5 focusing only on:
 * - SQL query is executed via parameterized PreparedStatement (no string concatenation of user input).
 *
 * These tests rely on mocking JDBC artifacts to verify that parameters are bound rather than
 * concatenated into the SQL string.
 */
class Assignment5Test {

    @Test
    void loginShouldUsePreparedStatementWithBoundParameters() throws Exception {
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
        when(flags.getFlag(5)).thenReturn("flag-5");

        String username = "Larry";
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Previously, the SQL was built by string concatenation of user input.
        // After the fix, the query must use placeholders and bind parameters.
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");

        // Ensure parameters are passed via bind variables, not concatenated.
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Also assert that the expected success path is still reachable.
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isTrue();
    }

    @Test
    void loginShouldShortCircuitWhenUsernameIsNotLarryWithoutDbAccess() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("NotLarry", "whatever");

        // Assert
        // Regression guard: validation branch should prevent any DB interaction.
        verifyNoInteractions(dataSource);
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }

    @Test
    void loginShouldShortCircuitWhenUsernameOrPasswordBlankWithoutDbAccess() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("", "");

        // Assert
        verifyNoInteractions(dataSource);
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
