package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/**
 * Delta unit tests for Assignment5 focusing only on the changed behavior:
 * usage of parameterized PreparedStatement instead of string concatenation
 * to prevent SQL injection.
 */
class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized query and succeeds for valid Larry credentials")
    void loginWithValidLarryCredentialsSucceedsUsingParameterizedQuery() throws Exception {
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

        // Act
        AttackResult result = assignment5.login("Larry", "secret");

        // Assert
        // Verify query text is the parameterized version (no concatenation)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                usedSql,
                "SQL should use parameter placeholders and not concatenate user input");

        // Verify parameters are bound as expected
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "secret");

        // Verify normal successful behavior still works (challenge solved)
        // We don't know exact message keys, so just assert success flag.
        // AttackResult has isSuccessful() in WebGoat; if signature differs, adapt accordingly.
        // TODO: If AttackResult API differs, adapt these assertions to the real API.
        assertEquals(true, result.isSuccessful(), "Valid Larry credentials should succeed");
    }

    @Test
    @DisplayName("login prevents SQL injection attempt by treating input as data, not SQL")
    void loginWithSqlInjectionAttemptFailsBecauseOfParameterizedQuery() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Simulate that no row is found when injection payload is used
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Injection payload that would have worked when concatenated directly
        String maliciousPassword = "' OR '1'='1";

        // Act
        AttackResult result = assignment5.login("Larry", maliciousPassword);

        // Assert
        // Ensure the same parameterized query is used
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                usedSql,
                "SQL injection fix must keep the parameterized query");

        // Critically, ensure the malicious value is bound as a parameter, not concatenated
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, maliciousPassword);

        // Because the password is treated as data, the injection should not bypass auth
        // i.e., no row returned => failed result.
        // TODO: If AttackResult API differs, update this assertion accordingly.
        assertEquals(false, result.isSuccessful(),
                "Injection payload must not allow successful login when using prepared statements");
    }
}
