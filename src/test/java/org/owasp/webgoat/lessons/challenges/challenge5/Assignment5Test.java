package org.owasp.webgoat.lessons.challenges.challenge5;

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
import org.springframework.web.bind.annotation.RequestParam;

class Assignment5Test {

    @Test
    @DisplayName("login() should use parameterized query and set user inputs as parameters")
    void login_usesParameterizedQuery_preventsSqlInjection() throws Exception {
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

        String username = "Larry' OR '1'='1";
        String password = "anything";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Verify prepared statement is created with placeholders instead of concatenated input
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        // Ensure the SQL contains placeholders and not raw user input
        org.junit.jupiter.api.Assertions.assertTrue(
                sql.contains("where userid = ? and password = ?"),
                "SQL must use parameter placeholders");
        org.junit.jupiter.api.Assertions.assertFalse(
                sql.contains(username),
                "SQL must not contain raw username value");
        org.junit.jupiter.api.Assertions.assertFalse(
                sql.contains(password),
                "SQL must not contain raw password value");

        // Verify that setString is used to bind parameters in correct order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Also ensure business logic still works (success path)
        org.junit.jupiter.api.Assertions.assertTrue(result.getLessonCompleted());
    }

    @Test
    @DisplayName("login() should fail when required parameters are missing")
    void login_missingParameters_returnsFailure() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result1 = assignment5.login("", "password");
        AttackResult result2 = assignment5.login("Larry", "");

        org.junit.jupiter.api.Assertions.assertFalse(result1.getLessonCompleted());
        org.junit.jupiter.api.Assertions.assertFalse(result2.getLessonCompleted());

        // No DB interaction should occur when required parameters are missing
        verifyNoInteractions(dataSource);
    }
}
