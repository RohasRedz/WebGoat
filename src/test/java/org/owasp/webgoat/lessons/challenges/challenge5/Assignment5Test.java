// Assuming standard Maven/Gradle test source root mapping from src/main/java to src/test/java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the changed SQL behavior:
 * - Ensures PreparedStatement with placeholders is used instead of string concatenation.
 * - Ensures user input is bound via setString and passed unmodified to the query.
 *
 * Note: We verify behavior by mocking JDBC classes and capturing interactions;
 * we do NOT assert on the literal SQL string containing user input.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized PreparedStatement and succeed on matching credentials")
    void login_usesPreparedStatementAndBindsParameters() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "p@ssw0rd' OR '1'='1"; // contains characters that previously allowed injection

        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1) PreparedStatement is created with a parameterized query (no concatenation with user input here).
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");

        // 2) User inputs are bound via setString in the correct order.
        InOrder inOrder = inOrder(preparedStatement);
        inOrder.verify(preparedStatement).setString(1, username);
        inOrder.verify(preparedStatement).setString(2, password);

        // 3) Query is executed and, when a row is present, success is returned with the flag.
        verify(preparedStatement).executeQuery();
        assertEquals("FLAG-5", result.getOutput()); // AttackResultBuilder adds flag as output

        verifyNoMoreInteractions(preparedStatement, connection);
    }

    @Test
    @DisplayName("login should fail when credentials do not match, even if password looks like injection")
    void login_failsOnNonMatchingCredentialsDespiteInjectionLikePassword() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "doesnotmatch' OR '1'='1";

        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Simulate no matching row in DB (credentials do not match)
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Ensure parameters are still bound and query executed
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        // Because there is no row, the login must fail (no bypass via injection)
        // We assert on the lesson-specific failure indicator (e.g., "challenge.close").
        // Since AttackResult API details are not fully known, we check 'lessonCompleted' flag.
        assertEquals(false, result.isLessonCompleted());

        verifyNoMoreInteractions(preparedStatement, connection);
    }
}
