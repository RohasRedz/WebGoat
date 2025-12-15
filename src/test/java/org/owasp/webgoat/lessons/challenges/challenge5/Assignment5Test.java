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
    @DisplayName("login should authenticate correctly when credentials are valid using parameterized SQL")
    void loginWithValidCredentialsShouldSucceed() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("flag-5");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("Larry", "correct-password");

        assertTrue(result.getLessonCompleted(), "Login with correct credentials should succeed after fix");
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "correct-password");
        verify(connection, never()).prepareStatement(contains("userid = '"));
        verify(connection, never()).prepareStatement(contains("password = '"));
    }

    @Test
    @DisplayName("login should fail for invalid password using parameterized SQL")
    void loginWithInvalidPasswordShouldFail() throws Exception {
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
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("Larry", "wrong-password");

        assertFalse(result.getLessonCompleted(), "Login with invalid credentials should fail after fix");
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "wrong-password");
    }

    @Test
    @DisplayName("login should reject non-Larry usernames regardless of SQL behavior")
    void loginWithNonLarryUserShouldShortCircuitBeforeSql() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("Bob", "any-password");

        assertFalse(result.getLessonCompleted(), "Non-Larry username should not be allowed");
        verify(dataSource, never()).getConnection();
    }
}
