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
import org.springframework.util.StringUtils;

/**
 * Delta unit tests for Assignment5 focusing only on the changed behavior:
 * - SQL query must now be parameterized (no string concatenation with user input).
 * - Login behavior remains the same for:
 *   * Missing credentials (failure 'required4').
 *   * Non-Larry username (failure 'user.not.larry').
 *   * Valid Larry credentials (success 'challenge.solved').
 *   * Invalid Larry credentials (failure 'challenge.close').
 *
 * NOTE: These tests do not attempt to cover the entire class behavior; they are
 * scoped to the security fix and its immediate behavioral impact.
 */
public class Assignment5DeltaTest {

    @Test
    @DisplayName("login() should use parameterized PreparedStatement and bind username and password")
    void loginShouldUseParameterizedPreparedStatement() throws Exception {
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
        when(resultSet.next()).thenReturn(true); // simulate successful login branch
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry";
        String password = "password123";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: verify SQL is parameterized (has placeholders) and parameters are bound
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        assertTrue(
                usedSql.contains("userid = ?"),
                "SQL should use a parameter placeholder for userid"
        );
        assertTrue(
                usedSql.contains("password = ?"),
                "SQL should use a parameter placeholder for password"
        );

        // verify parameters are bound in correct order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // also assert we still get a success result for valid credentials
        assertTrue(result.getLessonCompleted(), "Login should succeed for valid Larry credentials");
        assertEquals("challenge.solved", result.getFeedback(), "Feedback key should indicate challenge solved");
    }

    @Test
    @DisplayName("login() should fail when username or password is missing (required4)")
    void loginShouldFailWhenCredentialsMissing() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult noUser = assignment5.login("", "secret");
        AttackResult noPassword = assignment5.login("Larry", "  ");

        // Assert
        assertFalse(noUser.getLessonCompleted(), "Lesson must not be completed when username is missing");
        assertEquals("required4", noUser.getFeedback(), "Feedback should indicate missing required fields");

        assertFalse(noPassword.getLessonCompleted(), "Lesson must not be completed when password is missing");
        assertEquals("required4", noPassword.getFeedback(), "Feedback should indicate missing required fields");
    }

    @Test
    @DisplayName("login() should fail for non-Larry usernames (user.not.larry)")
    void loginShouldFailForNonLarryUser() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Alice", "whatever");

        // Assert
        assertFalse(result.getLessonCompleted(), "Lesson must not be completed for non-Larry user");
        assertEquals("user.not.larry", result.getFeedback(), "Feedback should indicate wrong user");
    }

    @Test
    @DisplayName("login() should fail when credentials for Larry are invalid (challenge.close)")
    void loginShouldFailWhenLarryCredentialsInvalid() throws Exception {
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
        when(resultSet.next()).thenReturn(false); // simulate invalid credentials

        // Act
        AttackResult result = assignment5.login("Larry", "wrong-password");

        // Assert
        assertFalse(result.getLessonCompleted(), "Lesson must not be completed for invalid Larry credentials");
        assertEquals("challenge.close", result.getFeedback(), "Feedback should indicate challenge not solved");
    }
}
