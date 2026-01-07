package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
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

class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized PreparedStatement and does not concatenate raw user input in SQL")
    void login_usesParameterizedQuery_noSqlInjection() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Simulate a successful login row returned
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String maliciousUsername = "Larry' OR '1'='1";
        String maliciousPassword = "anything' OR 'x'='x";

        // Act
        AttackResult result = assignment5.login(maliciousUsername, maliciousPassword);

        // Assert
        // 1) Verify query text uses placeholders instead of concatenating user input
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        assertThat(usedSql)
                .as("SQL should use parameter placeholders")
                .contains("userid = ?")
                .contains("password = ?");

        assertThat(usedSql)
                .as("SQL must not contain raw user input")
                .doesNotContain(maliciousUsername)
                .doesNotContain(maliciousPassword);

        // 2) Verify parameters are bound via setString and in expected order
        verify(preparedStatement).setString(1, maliciousUsername);
        verify(preparedStatement).setString(2, maliciousPassword);

        // 3) Behaviour-wise, the method should still execute without throwing
        //    and the statement must be executed.
        verify(preparedStatement).executeQuery();
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("login still succeeds for valid user with correct credentials after parameterization")
    void login_validUserStillWorks() throws Exception {
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
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "correct-password";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLesson()).isEqualTo(assignment5);
        // We only assert that it is a success result; the exact feedback text is outside delta scope.
        assertThat(result.getOutput()).contains("challenge.solved");

        // Ensure parameters were still correctly bound
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }
}
