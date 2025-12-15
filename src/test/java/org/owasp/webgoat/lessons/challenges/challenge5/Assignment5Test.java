package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.web.bind.MissingServletRequestParameterException;

/**
 * Delta test for Assignment5 focusing on the SQL-injection-related change:
 * ensuring a parameterized PreparedStatement is used and that SQL-injection-like
 * payloads do not alter behavior.
 */
class Assignment5Test {

    @Test
    @DisplayName("login should return failure when required parameters are missing")
    void loginMissingParametersReturnsFailure() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act & Assert
        // Missing parameter simulation: controller-level would normally reject this before,
        // but we ensure that if called with empty values the method enforces required fields.
        AttackResult result1 = assignment5.login("", "password");
        AttackResult result2 = assignment5.login("Larry", "");

        assertEquals("required4", result1.getFeedback(), "Expected required4 when username is empty");
        assertEquals("required4", result2.getFeedback(), "Expected required4 when password is empty");
    }

    @Test
    @DisplayName("login should fail when username is not Larry (precondition before DB use)")
    void loginNonLarryUserFailsBeforeDb() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Mallory", "anyPassword");

        // Assert
        assertEquals("user.not.larry", result.getFeedback(), "Non-Larry usernames should fail before DB use");
        // Verify DB is never hit when user is not Larry (focus on changed SQL path not being used)
        verify(dataSource, never()).getConnection();
    }

    @Test
    @DisplayName("login should use parameterized PreparedStatement and succeed for valid Larry credentials")
    void loginUsesParameterizedQueryAndSucceedsForValidUser() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("flag-5");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "correct-password");

        // Assert
        // Verify the SQL string uses parameter placeholders (delta behavior)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        // We only assert the essential invariant: the SQL must contain parameter placeholders and not
        // concatenate the username/password directly.
        // (We do not assert the entire query string to keep the test resilient to minor formatting changes.)
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL should use parameter placeholders and not embed user input directly");

        // Verify bound parameters correspond to the arguments (order is important for security fix)
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "correct-password");

        // Behavior: result should be success when DB says credentials are valid
        org.junit.jupiter.api.Assertions.assertTrue(result.isLessonCompleted(), "Expected success for valid credentials");
        org.junit.jupiter.api.Assertions.assertEquals("challenge.solved", result.getFeedback());
    }

    @Test
    @DisplayName("login should not be tricked by SQL-injection-like payloads due to parameterization")
    void loginSqlInjectionPayloadDoesNotAlterQuerySemantics() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("flag-5");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Simulate that DB does NOT find a matching row for injection payload
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // SQL-injection-like payload that would have broken concatenated SQL:
        String maliciousPassword = "' OR '1'='1";

        // Act
        AttackResult result = assignment5.login("Larry", maliciousPassword);

        // Assert
        // Ensure parameters are still bound as literal values, not altering the WHERE clause logic
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, maliciousPassword);

        org.junit.jupiter.api.Assertions.assertFalse(result.isLessonCompleted(), "Injection payload should not yield success");
        assertEquals("challenge.close", result.getFeedback(), "Expected regular failure feedback");
    }

    @Test
    @DisplayName("login should propagate SQL exceptions as generic exception from controller method")
    void loginPropagatesSqlException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB down"));

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act & Assert
        // The method declares 'throws Exception', so we just ensure an exception propagates,
        // proving the try-with-resources still works even when preparedStatement creation fails.
        assertThrows(Exception.class, () -> assignment5.login("Larry", "anyPassword"));
    }
}
