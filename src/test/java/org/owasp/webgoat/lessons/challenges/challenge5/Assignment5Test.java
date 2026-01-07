package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.*;
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
    @DisplayName("login should fail with 'required4' when username or password is missing")
    void login_missingUsernameOrPassword_returnsRequired4Failure() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult resultEmptyUser = assignment5.login("", "password");
        AttackResult resultEmptyPassword = assignment5.login("Larry", "");

        assertFalse(resultEmptyUser.isLessonSolved(), "Empty username should fail");
        assertTrue(resultEmptyUser.getFeedback().orElse("").contains("required4"),
                "Feedback should contain 'required4' for empty username");

        assertFalse(resultEmptyPassword.isLessonSolved(), "Empty password should fail");
        assertTrue(resultEmptyPassword.getFeedback().orElse("").contains("required4"),
                "Feedback should contain 'required4' for empty password");
    }

    @Test
    @DisplayName("login should fail with 'user.not.larry' when username is not Larry")
    void login_wrongUsername_returnsUserNotLarryFailure() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String nonLarryUser = "Bob";
        AttackResult result = assignment5.login(nonLarryUser, "somePassword");

        assertFalse(result.isLessonSolved(), "Non-Larry username should fail");
        String feedback = result.getFeedback().orElse("");
        assertTrue(feedback.contains("user.not.larry"),
                "Feedback should contain 'user.not.larry'");
        assertTrue(feedback.contains(nonLarryUser),
                "Feedback should include the provided username");
    }

    @Test
    @DisplayName("login should succeed for Larry with correct password using parameterized PreparedStatement")
    void login_correctCredentials_usesParameterizedQueryAndReturnsSuccess() throws Exception {
        // Arrange: mocks
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

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

        // Assert: business behavior
        assertTrue(result.isLessonSolved(), "Correct credentials for Larry should solve the challenge");
        assertTrue(result.getFeedback().orElse("").contains("challenge.solved"),
                "Feedback should contain 'challenge.solved'");
        assertTrue(result.getOutput().orElse("").contains("FLAG-5"),
                "Output should contain the flag value");

        // Assert: secure behavior  parameterized query with bound parameters
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "secret");
        verify(preparedStatement).executeQuery();

        // Ensure no other unexpected interactions that might indicate concatenated SQL
        verifyNoMoreInteractions(preparedStatement);
    }
}
