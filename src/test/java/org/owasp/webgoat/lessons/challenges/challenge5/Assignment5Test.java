// Assuming standard Maven/Gradle test source root and mirroring the main package.
// TODO: Adjust package if the project uses a different structure.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.*;
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
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * - SQL query must use parameterized PreparedStatement with placeholders.
 * - User input must be bound via setString (no concatenation into the SQL).
 */
class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized PreparedStatement and binds user inputs safely")
    void login_usesParameterizedQueryAndBindsParameters() throws Exception {
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

        Assignment5 endpoint = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "s3cret";

        // Act
        AttackResult result = endpoint.login(username, password);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        // Ensure the SQL contains placeholders instead of concatenated user input
        assertTrue(usedSql.contains("userid = ?"), "SQL should use placeholder for userid");
        assertTrue(usedSql.contains("password = ?"), "SQL should use placeholder for password");
        assertFalse(usedSql.contains(username), "SQL must not inline the username");
        assertFalse(usedSql.contains(password), "SQL must not inline the password");

        // Ensure parameters are bound via setString in correct order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Verify behavior is still successful for valid credentials
        assertNotNull(result);
        assertTrue(result.getLessonCompleted(), "Lesson should be completed when query returns a row");
    }

    @Test
    @DisplayName("login does not execute query when username is not Larry (unchanged guard behavior)")
    void login_guardStillPreventsNonLarryUsers() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        // connection must never be requested when username != Larry
        Assignment5 endpoint = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = endpoint.login("Mallory", "whatever");

        // Assert
        verify(dataSource, never()).getConnection();
        assertNotNull(result);
        assertFalse(result.getLessonCompleted(), "Non-Larry users should not complete the lesson");
    }
}
