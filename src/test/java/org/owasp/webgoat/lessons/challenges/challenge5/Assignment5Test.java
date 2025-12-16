package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment5;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        flags = mock(Flags.class);
        assignment5 = new Assignment5(dataSource, flags);

        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(flags.getFlag(5)).thenReturn("FLAG-5");
    }

    @Test
    @DisplayName("login should use parameterized PreparedStatement with placeholders for username and password")
    void login_usesParameterizedPreparedStatement() throws Exception {
        String username = "Larry";
        String password = "correctPassword";
        when(resultSet.next()).thenReturn(true);

        assignment5.login(username, password);

        InOrder inOrder = inOrder(connection, preparedStatement);
        inOrder.verify(connection)
                .prepareStatement("select password from challenge_users where userid = ? and password = ?");
        inOrder.verify(preparedStatement).setString(1, username);
        inOrder.verify(preparedStatement).setString(2, password);
        inOrder.verify(preparedStatement).executeQuery();

        verify(preparedStatement, times(2)).setString(anyInt(), anyString());
        verifyNoMoreInteractions(preparedStatement);
    }

    @Test
    @DisplayName("login should succeed only when username is Larry and password matches DB record")
    void login_succeedsForLarryWithCorrectPassword() throws Exception {
        String username = "Larry";
        String password = "correctPassword";
        when(resultSet.next()).thenReturn(true);

        AttackResult result = assignment5.login(username, password);

        assertEquals(true, result.getLessonCompleted(), "Expected challenge to be solved for correct credentials");
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("login should fail for Larry with incorrect password (no DB match)")
    void login_failsForLarryWithIncorrectPassword() throws Exception {
        String username = "Larry";
        String password = "wrongPassword";
        when(resultSet.next()).thenReturn(false);

        AttackResult result = assignment5.login(username, password);

        assertEquals(false, result.getLessonCompleted(), "Expected challenge not to be solved for incorrect password");
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("login should fail for non-Larry usernames even if DB would otherwise match")
    void login_failsForNonLarryUser() throws Exception {
        String username = "Mallory";
        String password = "somePassword";

        AttackResult result = assignment5.login(username, password);

        assertEquals(false, result.getLessonCompleted(), "Expected challenge not to be solved for non-Larry user");
        verifyNoInteractions(connection);
    }
}
