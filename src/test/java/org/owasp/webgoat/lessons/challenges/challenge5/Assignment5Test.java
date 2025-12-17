package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * - SQL query must be parameterized (no concatenation of user input into SQL string).
 * - SQLException is handled and translated into a failure AttackResult, not propagated.
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
        when(connection.prepareStatement(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void login_usesParameterizedQueryAndBindsUserInput() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "SecurePass123";
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("flag-5");

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());

        String usedSql = sqlCaptor.getValue();
        // ensure the SQL uses placeholders, not concatenated values
        // If the old vulnerable query were used, it would contain 'Larry' or the password directly.
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL should use parameter placeholders instead of concatenating user input");

        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // and ensure behavior remains successful for correct credentials
        org.junit.jupiter.api.Assertions.assertTrue(result.isLessonCompleted());
    }

    @Test
    void login_handlesSqlExceptionAndReturnsFailureWithoutPropagating() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "any";
        when(connection.prepareStatement(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new SQLException("DB down"));

        // Capture logger via reflection to assert that an error is logged
        Logger loggerMock = mock(Logger.class);
        ReflectionTestUtils.setField(assignment5, "log", loggerMock);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: method should not throw; it should return a failed AttackResult
        org.junit.jupiter.api.Assertions.assertFalse(result.isLessonCompleted(), "Should return failure when DB error occurs");

        // Verify logging of the database error
        verify(loggerMock).error(contains("Database error during login attempt"), org.mockito.ArgumentMatchers.any(SQLException.class));
    }

    @Test
    void login_rejectsSqlInjectionPayloadDueToParameterizedQuery() throws Exception {
        // Arrange: attempt a classical SQL injection payload
        String maliciousPassword = "' OR '1'='1";
        String username = "Larry";

        when(resultSet.next()).thenReturn(false); // no row should match exact password
        // Act
        AttackResult result = assignment5.login(username, maliciousPassword);

        // Assert: attack should fail, not log in
        org.junit.jupiter.api.Assertions.assertFalse(result.isLessonCompleted(), "Parameterized query should prevent injection-based login bypass");

        // Also ensure parameter binding treated payload as data, not SQL
        verify(preparedStatement).setString(2, maliciousPassword);
    }
}
