// Assumption: package is derived from the source file path.
// Source: src/main/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix:
 * - Verifies that a parameterized PreparedStatement with placeholders is used.
 * - Verifies that user input is bound via setString instead of string concatenation.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login() should use parameterized PreparedStatement and bind user inputs safely")
    void login_usesParameterizedQueryAndBindsParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Flags flags = Mockito.mock(Flags.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(
                        connection.prepareStatement(
                                Mockito.anyString()))
                .thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenReturn(true);
        Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "P@ssw0rd";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(connection)
                .prepareStatement(sqlCaptor.capture());

        String usedSql = sqlCaptor.getValue();
        // Ensure the SQL contains placeholders instead of concatenated values.
        assertTrue(
                usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                "SQL must use parameter placeholders for userid and password");

        // Ensure user input is not concatenated into the SQL string itself.
        assertTrue(
                !usedSql.contains(username) && !usedSql.contains(password),
                "User input must not be present directly in the SQL string");

        // Verify that parameters are bound using setString in correct order.
        Mockito.verify(preparedStatement).setString(1, username);
        Mockito.verify(preparedStatement).setString(2, password);

        // Sanity-check: behavior remains successful when a row is returned.
        assertTrue(result.getLessonCompleted(), "Login should succeed when resultSet has a row");
        assertEquals("FLAG-5", result.getOutput(), "Flag from Flags service should be returned");
    }

    @Test
    @DisplayName("login() should not execute query when username or password is empty")
    void login_doesNotHitDatabaseWhenInputMissing() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Flags flags = Mockito.mock(Flags.class);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult resultEmptyUser =
                assignment5.login("", "somePassword");
        AttackResult resultEmptyPassword =
                assignment5.login("Larry", "");

        // Assert
        Mockito.verifyNoInteractions(connection);
        assertTrue(resultEmptyUser.isFailed(), "Expected failure when username is empty");
        assertTrue(resultEmptyPassword.isFailed(), "Expected failure when password is empty");
    }
}
