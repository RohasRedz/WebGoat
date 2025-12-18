package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

public class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized PreparedStatement and returns success when row exists")
    void login_usesParameterizedQuery_successWhenResultExists() throws Exception {
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
        when(flags.getFlag(5)).thenReturn("flag-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("Larry", "secret");

        Mockito.verify(preparedStatement).setString(1, "Larry");
        Mockito.verify(preparedStatement).setString(2, "secret");
        Mockito.verify(preparedStatement).executeQuery();

        assertTrue(result.getLessonCompleted(), "Expected challenge to be solved when a row exists");
        assertEquals("flag-5", result.getFeedbackArgs().get(0),
                "Expected flag from Flags bean to be used in success feedback");
    }

    @Test
    @DisplayName("login still fails correctly when no matching row exists (logic preserved)")
    void login_usesParameterizedQuery_failureWhenNoRow() throws Exception {
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
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("Larry", "wrong");

        Mockito.verify(preparedStatement).setString(1, "Larry");
        Mockito.verify(preparedStatement).setString(2, "wrong");
        Mockito.verify(preparedStatement).executeQuery();

        assertFalse(result.getLessonCompleted(), "Expected challenge not to be solved when no row exists");
    }
}
