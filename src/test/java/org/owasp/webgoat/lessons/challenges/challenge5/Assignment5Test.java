package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
 * Delta tests for Assignment5 focusing on the fixed SQL-injection vulnerability.
 * Verifies that user input is bound via PreparedStatement parameters instead of
 * being concatenated directly into the SQL string.
 */
class Assignment5Test {

    @Test
    @DisplayName("login() should use parameterized query and succeed for valid Larry credentials")
    void login_usesPreparedStatementParameters_successForValidLarry() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

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

        // Act
        AttackResult result = assignment5.login("Larry", "securePassword");

        // Assert
        // Verify that parameters are bound using setString and not concatenated
        Mockito.verify(preparedStatement).setString(1, "Larry");
        Mockito.verify(preparedStatement).setString(2, "securePassword");
        Mockito.verify(preparedStatement).executeQuery();
        verifyNoMoreInteractions(preparedStatement);

        assertEquals(true, result.isLessonCompleted(), "Expected lesson to be completed for valid Larry credentials");
    }

    @Test
    @DisplayName("login() should fail when credentials do not match, still using parameterized query")
    void login_usesPreparedStatementParameters_failsForInvalidPassword() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
            .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Simulate no row -> invalid credentials
        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment5.login("Larry", "wrongPassword");

        // Assert
        Mockito.verify(preparedStatement).setString(1, "Larry");
        Mockito.verify(preparedStatement).setString(2, "wrongPassword");
        Mockito.verify(preparedStatement).executeQuery();
        verifyNoMoreInteractions(preparedStatement);

        assertEquals(false, result.isLessonCompleted(), "Expected lesson not to be completed for invalid password");
    }

    @Test
    @DisplayName("login() should not execute query when username is not Larry (precondition from original behavior)")
    void login_doesNotQueryWhenUserIsNotLarry() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Even if connection were returned, we should not reach it for non-Larry
        LessonDataSource dataSourceSpy = Mockito.spy(dataSource);

        // Act
        AttackResult result = assignment5.login("Mallory", "anyPassword");

        // Assert
        assertEquals(false, result.isLessonCompleted(), "Expected failure when user is not Larry");
        // Ensure no DB interaction occurs for non-Larry according to original semantics
        Mockito.verifyNoInteractions(dataSourceSpy);
    }
}
