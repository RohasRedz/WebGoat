// TODO: Adjust package to match project structure if necessary.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing strictly on the SQL-injection fix:
 * - PreparedStatement must use parameter placeholders instead of string concatenation.
 * - Correct behavior for valid vs invalid credentials when using the parameterized query.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized PreparedStatement and set user inputs as parameters")
    void login_usesParameterizedQueryAndSetsParameters() throws Exception {
        // Arrange
        DataSource realDataSource = mock(DataSource.class);
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(lessonDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

        String username = "Larry";
        String password = "secretPassword";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Verify that the SQL string contains placeholders (no direct concatenation of user input)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sqlUsed = sqlCaptor.getValue();
        assertTrue(
                sqlUsed.contains("userid = ?") && sqlUsed.contains("password = ?"),
                "SQL should use parameter placeholders instead of concatenating user input");

        // Verify that user inputs are passed as parameters, not concatenated into the SQL string
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Behavior: on matching row, challenge is solved
        assertTrue(result.getSuccess(), "Expected successful attack result when credentials match");
    }

    @Test
    @DisplayName("login should fail gracefully when credentials do not match using parameterized query")
    void login_invalidCredentialsFailWithParameterizedQuery() throws Exception {
        // Arrange
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(lessonDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // no matching row

        Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

        String username = "Larry";
        String password = "wrongPassword";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Still ensure placeholders are used
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sqlUsed = sqlCaptor.getValue();
        assertTrue(
                sqlUsed.contains("userid = ?") && sqlUsed.contains("password = ?"),
                "SQL should use parameter placeholders for invalid credentials as well");

        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Behavior: when no record is found, the challenge should not be solved
        assertTrue(result.getLessonCompleted() == false, "Invalid credentials should not complete the lesson");
    }
}
