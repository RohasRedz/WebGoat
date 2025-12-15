package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
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

/**
 * Delta unit tests for Assignment5 focusing ONLY on the behavior changed
 * by the SQL injection fix:
 *
 * 1) Ensuring the SQL query is parameterized (uses '?' placeholders) and
 *    does not embed raw username/password via string concatenation.
 * 2) Ensuring that SQL injection-style credentials no longer bypass
 *    authentication (the result remains failed).
 */
class Assignment5DeltaTest {

    @Test
    @DisplayName("login() should use parameterized SQL with placeholders, not concatenated user input")
    void loginShouldUseParameterizedSql() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);

        // Capture the SQL passed into prepareStatement
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "password123";

        // Act
        assignment5.login(username, password);

        // Assert
        // Verify that the SQL contains placeholders instead of concatenated user input.
        String usedSql = sqlCaptor.getValue();
        assertThat(usedSql)
                .as("SQL should use placeholders")
                .contains("userid = ?")
                .contains("password = ?");
        assertThat(usedSql)
                .as("SQL must not inline username")
                .doesNotContain(username);
        assertThat(usedSql)
                .as("SQL must not inline password")
                .doesNotContain(password);

        // Also verify that parameters are actually bound via setString()
        verify(preparedStatement).setString(eq(1), eq(username));
        verify(preparedStatement).setString(eq(2), eq(password));
    }

    @Test
    @DisplayName("SQL-injection-style credentials should not bypass authentication after fix")
    void sqlInjectionAttemptShouldNotBypassAuthentication() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Simulate that the query finds no matching row (no injection success)
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Typical SQL injection pattern that used to exploit concatenated SQL
        String injectionUsername = "Larry' OR '1'='1";
        String injectionPassword = "anything' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(injectionUsername, injectionPassword);

        // Assert
        // With parameterized SQL, this injection pattern must not succeed.
        assertThat(result)
                .as("SQL injection attempt should fail, not be treated as success")
                .extracting(AttackResult::getLessonCompleted)
                .isEqualTo(false);

        // Also verify that the exact (malicious) strings were bound as parameters,
        // confirming they are treated as data, not as part of the SQL syntax.
        verify(preparedStatement).setString(1, injectionUsername);
        verify(preparedStatement).setString(2, injectionPassword);
    }
}
