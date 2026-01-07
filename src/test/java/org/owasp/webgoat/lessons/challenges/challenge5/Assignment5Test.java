package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.owasp.webgoat.container.assignments.AttackResult.Status.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests focused on the changed behavior:
 * - Use of parameterized PreparedStatement instead of string concatenation.
 * - Preserved success/failure semantics for valid/invalid credentials.
 * - Protection against SQL injection payloads.
 */
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
    }

    @Test
    void login_withValidLarryCredentials_returnsSuccess() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("flag-5");

        AttackResult result = assignment5.login("Larry", "secret");

        assertEquals(SUCCESS, result.getStatus());
        assertTrue(result.getOutput().contains("flag-5"));

        // Verify parameterized query is used with correct bindings
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("userid = ?"), "SQL must use parameter placeholder for userid");
        assertTrue(sql.contains("password = ?"), "SQL must use parameter placeholder for password");

        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "secret");
    }

    @Test
    void login_withWrongPassword_returnsFailure() throws Exception {
        when(resultSet.next()).thenReturn(false);

        AttackResult result = assignment5.login("Larry", "wrong");

        assertEquals(FAILED, result.getStatus());
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "wrong");
    }

    @Test
    void login_withNonLarryUsername_shortCircuitsBeforeQuery() throws Exception {
        AttackResult result = assignment5.login("Bob", "anything");

        assertEquals(FAILED, result.getStatus());
        // Ensure no DB interaction when username is not "Larry"
        verifyNoInteractions(connection);
    }

    @Test
    void login_withSqlInjectionPayload_doesNotChangeSqlStructure() throws Exception {
        when(resultSet.next()).thenReturn(false);

        String evilUser = "Larry' OR '1'='1";
        String evilPass = "anything' OR '1'='1";

        assignment5.login(evilUser, evilPass);

        // Capture SQL and verify it still contains placeholders rather than concatenated payloads
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        assertFalse(sql.contains(evilUser), "SQL must not contain raw username input");
        assertFalse(sql.contains(evilPass), "SQL must not contain raw password input");
        assertTrue(sql.contains("userid = ?"), "SQL must still use parameter placeholder for userid");
        assertTrue(sql.contains("password = ?"), "SQL must still use parameter placeholder for password");

        // Verify parameters receive the potentially malicious input as data, not as SQL
        verify(preparedStatement).setString(1, evilUser);
        verify(preparedStatement).setString(2, evilPass);
    }
}
