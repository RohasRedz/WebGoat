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
 * Delta tests for Assignment5 focusing only on behavior changed by the SQL injection fix.
 *
 * These tests verify:
 *  - Normal success path with correct credentials.
 *  - Failure path with wrong credentials.
 *  - That SQL injection attempts are treated as data, not changing the query semantics
 *    (ensuring the use of parameterized PreparedStatement).
 */
class Assignment5Test {

    @Test
    @DisplayName("login should succeed for valid 'Larry' credentials")
    void loginSucceedsForValidCredentials() throws Exception {
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

        // Act
        AttackResult result = assignment5.login("Larry", "correct-password");

        // Assert
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "correct-password");
        verify(preparedStatement).executeQuery();
        assertEquals("FLAG-5", result.getFeedbackArgs()[0], "Should return the configured flag for challenge 5");
        // We don't assert exact feedback key string to keep this delta test focused on behavior around the SQL change
    }

    @Test
    @DisplayName("login should fail for invalid password while still using parameterized query")
    void loginFailsForInvalidPassword() throws Exception {
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
        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment5.login("Larry", "wrong-password");

        // Assert
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "wrong-password");
        verify(preparedStatement).executeQuery();
        // We only assert that it is not the success case (no flag) to avoid coupling to message texts.
        assertEquals(0, result.getFeedbackArgs().length, "Failure should not expose a flag");
    }

    @Test
    @DisplayName("login should treat SQL injection payload as data via PreparedStatement parameters")
    void loginIsResilientAgainstSqlInjectionAttempt() throws Exception {
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
        // No matching row should be found when using parameters correctly
        when(resultSet.next()).thenReturn(false);

        String injectionUser = "Larry' OR '1'='1";
        String injectionPassword = "anything' OR '1'='1";

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        AttackResult result = assignment5.login(injectionUser, injectionPassword);

        // Assert
        // Ensure the SQL string is the safe, parameterized one.
        verify(connection).prepareStatement(queryCaptor.capture());
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                queryCaptor.getValue(),
                "Query must use positional parameters, not string concatenation");

        // Ensure the injection payloads are bound as data to parameters, not concatenated.
        verify(preparedStatement).setString(1, injectionUser);
        verify(preparedStatement).setString(2, injectionPassword);
        verify(preparedStatement).executeQuery();

        // Because the payload is treated as data, the query should not match and the login should fail.
        assertEquals(0, result.getFeedbackArgs().length, "Injection payload should not result in a successful login");
    }
}
