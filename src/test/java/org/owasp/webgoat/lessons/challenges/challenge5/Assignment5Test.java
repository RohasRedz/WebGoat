package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
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

class Assignment5Test {

    @Test
    @DisplayName("login uses PreparedStatement parameters and succeeds for valid credentials")
    void login_usesPreparedStatementParameters_andSucceedsForValidCredentials() throws Exception {
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

        AttackResult result = assignment5.login("Larry", "secret");

        assertEquals("success", result.getLessonPhase().toString().toLowerCase());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sqlUsed = sqlCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(
                sqlUsed.contains("userid = ?") && sqlUsed.contains("password = ?"),
                "SQL must use parameter placeholders");

        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "secret");
        verify(preparedStatement, times(2)).setString(anyInt(), anyString());
    }

    @Test
    @DisplayName("login fails for invalid credentials while still using parameterized PreparedStatement")
    void login_failsForInvalidCredentials_usingParameterizedPreparedStatement() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("Larry", "wrong");

        assertEquals("failure", result.getLessonPhase().toString().toLowerCase());

        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "wrong");
    }

    @Test
    @DisplayName("login rejects empty username or password (unchanged guard, here to anchor delta behavior)")
    void login_rejectsEmptyParameters_stillGuardedBeforeSQL() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("", "somePass");

        assertEquals("failure", result.getLessonPhase().toString().toLowerCase());
        verifyNoInteractions(dataSource);
    }
}
