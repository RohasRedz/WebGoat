package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.*;
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

/**
 * Delta tests focusing only on the changed behavior:
 * - Use of parameterized PreparedStatement with placeholders instead of string concatenation.
 * - Ensuring that SQL injection-style input does not bypass authentication.
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
        when(flags.getFlag(5)).thenReturn("FLAG-5");
    }

    @Test
    @DisplayName("login uses parameterized PreparedStatement and succeeds for valid Larry credentials")
    void login_usesParameterizedQuery_andSucceedsForValidUser() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "secret";
        when(resultSet.next()).thenReturn(true);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: verify parameterized SQL is used
        InOrder inOrder = inOrder(connection, preparedStatement);
        inOrder.verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        inOrder.verify(preparedStatement).setString(1, username);
        inOrder.verify(preparedStatement).setString(2, password);
        inOrder.verify(preparedStatement).executeQuery();

        assertTrue(result.getLessonCompleted(), "Expected challenge to be marked as solved");
    }

    @Test
    @DisplayName("login does not allow SQL injection payload to bypass authentication")
    void login_doesNotAllowSqlInjectionBypass() throws Exception {
        // Arrange
        String username = "Larry";
        String injectionPassword = "' OR '1'='1";
        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment5.login(username, injectionPassword);

        // Assert: still uses prepared statement with placeholders
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, injectionPassword);
        verify(preparedStatement).executeQuery();

        assertFalse(result.getLessonCompleted(), "SQL injection-style password must not grant access");
    }
}
