package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on the SQL parameterization change.
 *
 * These tests specifically assert that:
 * - A PreparedStatement with the expected parameterized SQL is used.
 * - User input (including characters typical of SQL injection) is passed as
 *   parameters and not concatenated into the SQL string.
 *
 * Note: We do not execute a real database; we only validate interaction with
 * JDBC APIs to ensure secure usage after the fix.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized PreparedStatement and treats input as data (no SQL concatenation)")
    void loginUsesParameterizedQueryAndPassesUserInputAsParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Flags flags = Mockito.mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Use a value that would be dangerous if concatenated directly into SQL
        String username = "Larry";
        String password = "pass' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Verify PreparedStatement-based query (no concatenated SQL string)
        Mockito.verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");

        // Verify that user-controlled values are bound as parameters
        Mockito.verify(preparedStatement).setString(1, username);
        Mockito.verify(preparedStatement).setString(2, password);

        // Ensure query was executed and login succeeded under normal (mocked) conditions
        Mockito.verify(preparedStatement).executeQuery();
        assertTrue(result.getLessonCompleted());
        assertTrue(result.getOutput().contains("FLAG-5"));
    }

    @Test
    @DisplayName("login rejects non-Larry usernames before reaching SQL layer")
    void loginRejectsNonLarryBeforeSql() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Mallory", "anyPassword");

        // Assert
        // This ensures precondition logic still short-circuits and thus
        // user-controlled usernames are not used in SQL at all.
        assertTrue(result.getOutput().contains("user.not.larry"));
        assertEquals(false, result.getLessonCompleted());
        Mockito.verifyNoInteractions(dataSource);
    }
}
