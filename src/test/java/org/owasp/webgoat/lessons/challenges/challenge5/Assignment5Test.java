package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

public class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized query and bind username and password correctly")
    void login_usesPreparedStatementWithBoundParameters() throws Exception {
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
        String password = "safePassword";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        // Ensure the SQL no longer contains concatenated user input but uses placeholders
        assertTrue(usedSql.contains("userid = ?"), "SQL should use placeholder for userid");
        assertTrue(usedSql.contains("password = ?"), "SQL should use placeholder for password");
        assertTrue(!usedSql.contains(username), "SQL must not directly contain username literal");
        assertTrue(!usedSql.contains(password), "SQL must not directly contain password literal");

        // Ensure parameters are bound in the expected order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // And login still succeeds when query returns a row
        assertTrue(result.isLessonCompleted(), "Login should still succeed when credentials match");
    }

    @Test
    @DisplayName("login should fail when username is not Larry (behavior preserved after fix)")
    void login_behaviorPreservedForNonLarryUser() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Mallory", "any");

        // Assert
        assertTrue(!result.isLessonCompleted(), "Non-Larry user must still not pass the challenge");
    }

    @Test
    @DisplayName("login should reject empty username or password (input validation unchanged)")
    void login_rejectsEmptyInputs() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result1 = assignment5.login("", "pwd");
        AttackResult result2 = assignment5.login("Larry", "");

        // Assert
        assertTrue(!result1.isLessonCompleted(), "Empty username must be rejected");
        assertTrue(!result2.isLessonCompleted(), "Empty password must be rejected");
    }
}
