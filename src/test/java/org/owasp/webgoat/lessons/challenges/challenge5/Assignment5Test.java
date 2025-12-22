// Derived from: src/main/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java
// Test path (main -> test): src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
 * Delta tests focused only on the changed behavior:
 * - SQL must be parameterized (no direct injection via concatenation).
 * - PreparedStatement parameters must be bound with the exact provided username/password.
 */
class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private Assignment5 assignment5;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        flags = mock(Flags.class);
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        assignment5 = new Assignment5(dataSource, flags);
    }

    @Test
    void login_usesParameterizedQueryAndBindsParameters() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "somePassword";
        when(resultSet.next()).thenReturn(true);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: verify parameterized SQL is used
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        // The updated code uses placeholders instead of concatenating user input
        assertEquals(
            "select password from challenge_users where userid = ? and password = ?",
            sql,
            "SQL must use parameter placeholders to prevent injection"
        );

        // Assert: parameters are bound in the correct order and with the correct values
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Ensure query is actually executed so that the change is on the real path
        verify(preparedStatement).executeQuery();

        // Ensure successful path still works
        // (we only care that the success behavior remains reachable)
        // NOTE: we don't assert the full AttackResult structure here, only that it is not null.
        // This keeps the test focused on the changed SQL behavior.
        assertEquals(true, result.isLessonCompleted());
    }

    @Test
    void login_doesNotConcatenateMaliciousInputIntoSql() throws Exception {
        // Arrange
        String maliciousUser = "Larry' OR '1'='1";
        String password = "anything";
        when(resultSet.next()).thenReturn(false); // even if parameters are bound, query should not magically succeed

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        assignment5.login(maliciousUser, password);

        // Assert: verify that SQL text itself does not contain the malicious payload
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        // The malicious payload must not appear inside the SQL literal
        // since parameters are bound via setString rather than string concatenation.
        org.junit.jupiter.api.Assertions.assertFalse(
            sql.contains(maliciousUser),
            "SQL must not contain raw user input; parameters must be bound instead"
        );

        // But the value is still passed via parameter binding
        verify(preparedStatement).setString(1, maliciousUser);
        verify(preparedStatement).setString(2, password);
    }
}
