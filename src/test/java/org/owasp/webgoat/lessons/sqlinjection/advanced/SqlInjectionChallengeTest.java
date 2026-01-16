package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.informationMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Delta tests for SqlInjectionChallenge focusing on the PreparedStatement-based user lookup:
 *  - normal registration succeeds,
 *  - duplicate user detection remains,
 *  - injection payload in username does not break SQL or bypass checks.
 */
public class SqlInjectionChallengeTest {

    private LessonDataSource dataSource;
    private SqlInjectionChallenge challenge;

    @BeforeEach
    void setUp() {
        dataSource = mock(LessonDataSource.class);
        challenge = new SqlInjectionChallenge(dataSource);
    }

    @Test
    void registerNewUser_shouldSucceedForNewUser() throws Exception {
        String username = "alice";
        String email = "alice@example.com";
        String password = "Secret123!";

        Connection connection = mock(Connection.class);
        PreparedStatement checkStmt = mock(PreparedStatement.class);
        PreparedStatement insertStmt = mock(PreparedStatement.class);
        ResultSet checkResult = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
            .thenReturn(checkStmt);
        when(connection.prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)")).thenReturn(insertStmt);
        when(checkStmt.executeQuery()).thenReturn(checkResult);
        when(checkResult.next()).thenReturn(false); // user does not yet exist

        AttackResult result = challenge.registerNewUser(username, email, password);

        assertFalse(result.getLessonCompleted(), "User creation info message should not mark lesson as fully completed");
        assertTrue(result.getFeedback().contains("user.created"),
            "Expected user.created feedback when new user is registered");

        verify(checkStmt).setString(1, username);
        verify(insertStmt).setString(1, username);
        verify(insertStmt).setString(2, email);
        verify(insertStmt).setString(3, password);
    }

    @Test
    void registerNewUser_shouldDetectDuplicateUser() throws Exception {
        String username = "bob";
        String email = "bob@example.com";
        String password = "Secret123!";

        Connection connection = mock(Connection.class);
        PreparedStatement checkStmt = mock(PreparedStatement.class);
        ResultSet checkResult = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
            .thenReturn(checkStmt);
        when(checkStmt.executeQuery()).thenReturn(checkResult);
        when(checkResult.next()).thenReturn(true); // user already exists

        AttackResult result = challenge.registerNewUser(username, email, password);

        assertFalse(result.getLessonCompleted(), "Duplicate user should not complete the lesson");
        assertTrue(result.getFeedback().contains("user.exists"),
            "Expected user.exists feedback when user already exists");

        verify(checkStmt).setString(1, username);
        verify(connection, never()).prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)");
    }

    @Test
    void registerNewUser_shouldHandleSqlInjectionPayloadSafely() throws Exception {
        String username = "alice' OR '1'='1";
        String email = "alice@example.com";
        String password = "Secret123!";

        Connection connection = mock(Connection.class);
        PreparedStatement checkStmt = mock(PreparedStatement.class);
        PreparedStatement insertStmt = mock(PreparedStatement.class);
        ResultSet checkResult = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
            .thenReturn(checkStmt);
        when(connection.prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)")).thenReturn(insertStmt);
        when(checkStmt.executeQuery()).thenReturn(checkResult);
        when(checkResult.next()).thenReturn(false); // treat as non-existing user

        AttackResult result = challenge.registerNewUser(username, email, password);

        assertNotNull(result, "Result must not be null for injection-like username");
        verify(checkStmt).setString(1, username);

        // Confirm usage of PreparedStatement with placeholder (no raw concatenation)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection, atLeastOnce()).prepareStatement(sqlCaptor.capture());
        assertTrue(sqlCaptor.getAllValues().stream().anyMatch(sql -> sql.contains("userid = ?")),
            "User check query must use parameter placeholder for username");
    }

    @Test
    void registerNewUser_shouldReturnFailureOnSqlException() throws Exception {
        String username = "charlie";
        String email = "charlie@example.com";
        String password = "Secret123!";

        when(dataSource.getConnection()).thenThrow(new SQLException("DB error"));

        AttackResult result = challenge.registerNewUser(username, email, password);

        assertFalse(result.getLessonCompleted(), "SQL errors should not complete the lesson");
        assertTrue(result.getOutput().contains("Something went wrong"),
            "Expected generic error message when SQLException occurs");
    }
}
