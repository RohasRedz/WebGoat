// Batch 1 - Derived test path: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
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
 * Delta tests for Assignment5 focusing on the secure SQL usage and authentication
 * behavior introduced by the fix:
 * - Uses parameterized queries instead of string concatenation.
 * - Correctly authenticates valid user "Larry" with valid password.
 * - Rejects invalid credentials.
 */
public class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment5;

    // Mocks for JDBC
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
    void login_usesParameterizedQueryAndAuthenticatesLarry() throws Exception {
        // Arrange
        when(resultSet.next()).thenReturn(true);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        AttackResult result = assignment5.login("Larry", "secret");

        // Assert: authentication succeeds
        assertTrue(result.isLessonCompleted(), "Expected challenge to be solved for valid Larry login");

        // Assert: SQL uses parameter placeholders instead of concatenated user input
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                usedSql,
                "SQL must use parameter placeholders to prevent SQL injection"
        );

        // Assert: parameters are bound via setString
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "secret");
    }

    @Test
    void login_rejectsInvalidCredentials() throws Exception {
        // Arrange
        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment5.login("Larry", "wrong");

        // Assert: authentication fails
        assertTrue(result.isFailed(), "Expected login to fail for invalid password");

        // Still ensure parameters are bound correctly for invalid credentials
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "wrong");
    }

    @Test
    void login_rejectsNonLarryUserBeforeQueryExecution() throws Exception {
        // Arrange
        // No DB interaction should be required for non-Larry

        // Act
        AttackResult result = assignment5.login("Bob", "anything");

        // Assert
        assertTrue(result.isFailed(), "Non-Larry users should be rejected");
        // Ensure the DB is never called for non-Larry
        verifyNoInteractions(connection);
    }
}
