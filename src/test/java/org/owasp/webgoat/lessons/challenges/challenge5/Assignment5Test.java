package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

@ExtendWith(MockitoExtension.class)
class Assignment5Test {

    @Mock private LessonDataSource dataSource;
    @Mock private Flags flags;
    @Mock private Connection connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet resultSet;
    @Captor private ArgumentCaptor<String> sqlCaptor;

    @InjectMocks private Assignment5 assignment5;

    @Test
    @DisplayName("login should use parameterized query and set parameters in order for valid credentials")
    void login_usesParameterizedQuery_andSetsParameters_forValidCredentials() throws Exception {
        String username = "Larry";
        String password = "secretPassword";

        org.mockito.Mockito.when(dataSource.getConnection()).thenReturn(connection);
        org.mockito.Mockito.when(connection.prepareStatement(sqlCaptor.capture()))
                .thenReturn(preparedStatement);
        org.mockito.Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        org.mockito.Mockito.when(resultSet.next()).thenReturn(true);
        org.mockito.Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

        AttackResult result = assignment5.login(username, password);

        String usedSql = sqlCaptor.getValue();
        String expectedFragment =
                "select password from challenge_users where userid = ? and password = ?";
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.replaceAll("\\s+", " ").contains(expectedFragment),
                "SQL should use parameter placeholders instead of concatenating user input");

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(preparedStatement);
        inOrder.verify(preparedStatement).setString(1, username);
        inOrder.verify(preparedStatement).setString(2, password);

        assertEquals(true, result.getLessonCompleted());
    }

    @Test
    @DisplayName("login should still fail cleanly with invalid password while using parameterization")
    void login_usesParameterizedQuery_forInvalidPassword_andFails() throws Exception {
        String username = "Larry";
        String wrongPassword = "wrongPassword";

        org.mockito.Mockito.when(dataSource.getConnection()).thenReturn(connection);
        org.mockito.Mockito.when(connection.prepareStatement(sqlCaptor.capture()))
                .thenReturn(preparedStatement);
        org.mockito.Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        org.mockito.Mockito.when(resultSet.next()).thenReturn(false);

        AttackResult result = assignment5.login(username, wrongPassword);

        String usedSql = sqlCaptor.getValue();
        String expectedFragment =
                "select password from challenge_users where userid = ? and password = ?";
        org.junit.jupiter.api.Assertions.assertTrue(
                usedSql.replaceAll("\\s+", " ").contains(expectedFragment),
                "SQL should use parameter placeholders for all authentication attempts");

        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, wrongPassword);

        assertEquals(false, result.getLessonCompleted());
    }

    @Test
    @DisplayName("login should not build SQL containing raw username or password literals")
    void login_doesNotConcatenateUserInputIntoSql() throws Exception {
        String username = "Larry' OR '1'='1";
        String password = "anything";

        org.mockito.Mockito.when(dataSource.getConnection()).thenReturn(connection);
        org.mockito.Mockito.when(connection.prepareStatement(sqlCaptor.capture()))
                .thenReturn(preparedStatement);
        org.mockito.Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        org.mockito.Mockito.when(resultSet.next()).thenReturn(false);

        assignment5.login(username, password);

        String usedSql = sqlCaptor.getValue();

        org.junit.jupiter.api.Assertions.assertFalse(
                usedSql.contains(username),
                "Username must not be concatenated directly into the SQL query");
        org.junit.jupiter.api.Assertions.assertFalse(
                usedSql.contains(password),
                "Password must not be concatenated directly into the SQL query");
    }
}
