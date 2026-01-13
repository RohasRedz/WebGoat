package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix:
 * ensuring PreparedStatement with parameters is used and that
 * user input is not concatenated into the SQL string.
 *
 * Derived test file path (per rules):
 * src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 */
public class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized PreparedStatement and not concatenate user input in SQL")
    void login_usesParameterizedQuery_andBindsParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("dummy-flag");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry' OR '1'='1";
        String password = "anything' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        String usedSql = sqlCaptor.getValue();
        // Ensure the query uses placeholders and not raw user input
        // The fixed code: "select password from challenge_users where userid = ? and password = ?"
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL should use parameter placeholders instead of directly embedding user input");

        org.junit.jupiter.api.Assertions.assertFalse(
                usedSql.contains(username) || usedSql.contains(password),
                "SQL must not contain raw user input to avoid SQL injection");

        // Verify parameter binding order and values
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Also assert that successful path still works
        assertEquals("success", result.getLessonStatus().toString().toLowerCase());
    }
}
