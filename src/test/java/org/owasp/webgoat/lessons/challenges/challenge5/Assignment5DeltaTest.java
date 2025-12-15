package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.*;
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
import org.springframework.util.StringUtils;

/**
 * Delta unit tests for Assignment5 focusing only on the behavior changed by the security fix:
 * 1) The SQL query must be executed via a parameterized PreparedStatement with bind variables.
 * 2) Typical SQL injection payloads such as "' OR '1'='1" must not bypass authentication when
 *    the backing data contains only normal credential rows.
 *
 * NOTE:
 * - These tests are intentionally scoped to the new secure behavior and do not attempt to
 *   exhaustively test all existing logic.
 */
public class Assignment5DeltaTest {

    @Test
    @DisplayName("login should use parameterized PreparedStatement with bind parameters")
    void login_usesParameterizedPreparedStatement() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        String username = "Larry";
        String password = "password123";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertNotNull(result, "AttackResult must not be null");

        // Verify that a PreparedStatement was created with parameter placeholders
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertTrue(usedSql.toLowerCase().contains("where userid = ? and password = ?"),
                "SQL must use parameter placeholders instead of string concatenation");

        // Verify that the user input values are bound via setString, not concatenated into SQL
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // We also implicitly verify no executeQuery() calls are made on a Statement built
        // from concatenated SQL, since only PreparedStatement is mocked and used here.
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("SQL injection style input must not bypass authentication")
    void login_sqlInjectionInputDoesNotSucceed() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Simulate a database that has only normal credential rows and does NOT match the injection input.
        // Earlier vulnerable code might have returned a row due to manipulated WHERE clause,
        // but with parameter binding, this should not happen.
        when(resultSet.next()).thenReturn(false);

        String maliciousUsername = "Larry"; // valid expected username
        String maliciousPassword = "' OR '1'='1"; // typical SQL injection payload

        // Sanity check on validation behavior (unchanged business logic)
        assertTrue(StringUtils.hasText(maliciousUsername));
        assertTrue(StringUtils.hasText(maliciousPassword));

        // Act
        AttackResult result = assignment5.login(maliciousUsername, maliciousPassword);

        // Assert
        assertNotNull(result, "AttackResult must not be null for SQL injection input");
        assertFalse(result.isLessonSolved(),
                "SQL injection-style password must not cause the challenge to be solved");

        // Verify the parameter binding still happens with the injection payload
        verify(preparedStatement).setString(1, maliciousUsername);
        verify(preparedStatement).setString(2, maliciousPassword);

        // Additionally, check that the SQL text still uses placeholders (defensive regression guard)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertTrue(usedSql.toLowerCase().contains("where userid = ? and password = ?"),
                "Parameterized SQL with placeholders must be used even for injection-style input");
    }
}
