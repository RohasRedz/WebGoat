package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

    @Test
    @DisplayName("login uses PreparedStatement parameters instead of string concatenation for SQL query")
    void login_usesParameterizedQueryAndPreventsSqlInjection() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("dummy-flag");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "pass' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1) Ensure a parameterized query is used (PreparedStatement with ? placeholders).
        var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL should use parameter placeholders instead of concatenating user input");

        // 2) Ensure user input is bound as parameters, not concatenated into SQL.
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // 3) Sanity check that the call still succeeds when DB returns a row.
        assertTrue(result.getLessonCompleted(), "Login should succeed when the DB returns a matching row");
    }

    @Test
    @DisplayName("login fails when username is not Larry (unchanged behavior, regression guard around new SQL)")
    void login_nonLarryStillFailsAfterSqlFix() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Bob", "anything");

        // Assert
        // Ensures logic around the new SQL path did not alter this precondition.
        assertEquals(false, result.getLessonCompleted(), "Non-Larry users should still fail login");
    }
}
