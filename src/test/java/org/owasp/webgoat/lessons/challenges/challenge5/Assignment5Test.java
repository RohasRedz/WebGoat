package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests focused on the change from stringbuilt SQL to parameterized
 * PreparedStatement in Assignment5.login(...).
 *
 * These tests:
 * - Verify that user input is bound as parameters, not concatenated into SQL.
 * - Verify that normal login flow still works when DB returns a row.
 * - Verify that an SQLinjection style password no longer results in success.
 */
class Assignment5Test {

    @Test
    @DisplayName("login uses PreparedStatement parameters and succeeds when resultSet has row")
    void login_usesPreparedStatementParameters_andSucceedsWhenRowExists() throws Exception {
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
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Act
        AttackResult result = assignment5.login("Larry", "safePassword");

        // Assert
        // Verify correct SQL with placeholders is used
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");

        // Verify that user input is bound as parameters, not concatenated
        InOrder inOrder = inOrder(preparedStatement);
        inOrder.verify(preparedStatement).setString(1, "Larry");
        inOrder.verify(preparedStatement).setString(2, "safePassword");
        verify(preparedStatement).executeQuery();

        // Ensure that the attack result is a success when a row exists
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).as("Expected challenge solved").isTrue();
    }

    @Test
    @DisplayName("login does not treat SQL injection string in password as successful login")
    void login_doesNotAllowSqlInjectionViaPassword() throws Exception {
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

        // Simulate that the query returns no rows for the injected password
        when(resultSet.next()).thenReturn(false);

        String maliciousPassword = "' OR '1'='1";

        // Act
        AttackResult result = assignment5.login("Larry", maliciousPassword);

        // Assert
        // Ensure parameters are bound exactly as provided (no SQL concatenation)
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, maliciousPassword);

        // Since no rows returned, the login must fail
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).as("SQL injection should not bypass authentication").isFalse();
    }
}
