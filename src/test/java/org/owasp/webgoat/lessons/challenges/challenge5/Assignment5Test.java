package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing only on the SQL injection fix:
 * - Verifies that the PreparedStatement SQL uses parameter placeholders.
 * - Verifies that user input is no longer concatenated into the SQL string.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login() should use parameterized PreparedStatement and not concatenate user input into SQL")
    void login_usesParameterizedQueryAndBindsUserInput() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "secretPass";

        // Mock JDBC artifacts
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);

        // Capture SQL passed to prepareStatement
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenReturn(true); // simulate successful login

        Flags flags = Mockito.mock(Flags.class);
        Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1) SQL must use placeholders and not contain raw username/password
        String usedSql = sqlCaptor.getValue();
        // Expected from fixed file:
        // "select password from challenge_users where userid = ? and password = ?"
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                usedSql,
                "SQL must use parameter placeholders instead of concatenating user input");

        // 2) Verify that user input is bound via parameters, not inlined
        Mockito.verify(preparedStatement).setString(1, username);
        Mockito.verify(preparedStatement).setString(2, password);

        // 3) Ensure functional behavior remains: successful login returns success AttackResult
        //    We assert that no exception is thrown and that executeQuery was invoked.
        Mockito.verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("login() should reject empty username or password before preparing SQL")
    void login_rejectsEmptyInputBeforeSqlExecution() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult resultEmptyUser = assignment5.login("", "pw");
        AttackResult resultEmptyPassword = assignment5.login("Larry", "");

        // Assert
        // Even though this behavior existed before, it protects the fixed SQL from being reached
        // with empty inputs; we assert that no SQL is executed (no interaction with dataSource).
        Mockito.verifyNoInteractions(dataSource);
    }

    @Test
    @DisplayName("login() should enforce Larry-only check before hitting SQL layer")
    void login_enforcesLarryUserGateBeforeSql() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Mallory", "any");

        // Assert
        // Ensure the Larry-only check still short-circuits before any SQL is executed.
        Mockito.verifyNoInteractions(dataSource);
    }
}
