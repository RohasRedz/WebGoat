// Delta_UnitTest_Agent
// NOTE: Package inferred from production class.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Delta unit tests for Assignment5 (Jira: SVCF-657).
 *
 * Focus only on changed behavior:
 *  - SQL must no longer be constructed via string concatenation.
 *  - PreparedStatement must use parameter placeholders and bind parameters safely.
 */
public class Assignment5DeltaTest {

    @Test
    @DisplayName("Login uses parameterized PreparedStatement to prevent SQL injection")
    void loginUsesParameterizedQuery() throws Exception {
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
        String password = "s3cret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert - verify SQL query uses placeholders (parameterized)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        assertThat(usedSql)
                .as("SQL must use placeholders for parameters, not string concatenation")
                .contains("userid = ?")
                .contains("password = ?")
                .doesNotContain(username)
                .doesNotContain(password);

        // Assert - parameters are bound via setString
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Assert - behavior remains correct on successful authentication
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isTrue();
    }

    @Test
    @DisplayName("Login rejects SQL injection payload because query is parameterized")
    void loginRejectsSqlInjectionPayload() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Simulate that query does not return any row when payload is treated as data
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String maliciousPassword = "' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(username, maliciousPassword);

        // Assert: should not be considered successful
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted())
                .as("SQL injection payload must not bypass authentication")
                .isFalse();

        // Also ensure it was still bound as a single parameter value
        verify(preparedStatement).setString(2, maliciousPassword);
    }

    @Test
    @DisplayName("Login still enforces non-empty username and password (unchanged behavior)")
    void loginStillValidatesInputPresence() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("", "");

        // Regression: ensure precondition has not been broken by SQL change
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }

    @Test
    @DisplayName("Login still restricts username to 'Larry' as per original logic")
    void loginStillRequiresLarryUser() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("Eve", "whatever");

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
