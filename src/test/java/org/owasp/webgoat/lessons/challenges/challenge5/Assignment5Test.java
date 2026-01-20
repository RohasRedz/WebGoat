// File: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta unit tests focused on the updated secure behavior:
 * - Use of PreparedStatement instead of string-concatenated SQL.
 * - Proper handling of SQLException via error feedback.
 */
class Assignment5Test {

    @Test
    @DisplayName("login uses PreparedStatement with parameters and succeeds on valid credentials")
    void login_usesPreparedStatement_andReturnsSuccessOnValidCredentials() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "secret");

        // Assert
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "secret");
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();

        // The exact feedback key is not exposed; assert that the result is marked as successful
        assertEquals(true, result.getLessonCompleted(), "Expected challenge to be marked as solved");
    }

    @Test
    @DisplayName("login returns database error feedback when SQLException occurs")
    void login_returnsDatabaseErrorFeedbackOnSQLException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB down"));

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "secret");

        // Assert
        // When a SQLException occurs, the method should not propagate the exception
        // and should instead return a failed AttackResult with database error feedback.
        assertEquals(false, result.getLessonCompleted(), "Expected challenge not to be solved on DB error");
        // The feedback key "error.database" is part of the new secure behavior
        // exposed via the message key in the AttackResult.
        // We use toString()/getFeedback to avoid tight coupling to internal representation.
        String resultString = result.toString();
        // basic containment check for the new feedback key
        org.junit.jupiter.api.Assertions.assertTrue(
                resultString.contains("error.database"),
                "Expected feedback to contain 'error.database' when SQLException occurs");
    }
}
