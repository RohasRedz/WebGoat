package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment5;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = Mockito.mock(LessonDataSource.class);
        flags = Mockito.mock(Flags.class);
        assignment5 = new Assignment5(dataSource, flags);

        connection = Mockito.mock(Connection.class);
        preparedStatement = Mockito.mock(PreparedStatement.class);
        resultSet = Mockito.mock(ResultSet.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(
                connection.prepareStatement(Mockito.anyString())
        ).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void login_usesParameterizedQueryAndReturnsSuccessForValidUser() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "secret";
        Mockito.when(resultSet.next()).thenReturn(true);
        Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: verify SQL text uses placeholders and not string concatenation
        Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        assertTrue(
                sql.contains("userid = ?") && sql.contains("password = ?"),
                "SQL must use parameter placeholders instead of concatenating user input"
        );

        // Assert: verify user-controlled values are bound via setString
        Mockito.verify(preparedStatement).setString(1, username);
        Mockito.verify(preparedStatement).setString(2, password);

        // Assert: verify successful attack result behavior is preserved
        assertTrue(result.getLessonCompleted(), "Expected challenge to be marked as solved");
        assertEquals("FLAG-5", result.getOutput(), "Expected flag to be returned on success");
    }

    @Test
    void login_failsForWrongUserAndDoesNotHitDatabase() throws Exception {
        // Arrange
        String username = "NotLarry";
        String password = "whatever";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: verify early validation still blocks non-Larry users
        assertTrue(!result.getLessonCompleted(), "Expected challenge not to be solved for non-Larry user");

        // Assert: verify no SQL is executed when user is invalid
        Mockito.verifyNoInteractions(connection);
    }

    @Test
    void login_failsWhenUsernameOrPasswordBlank() throws Exception {
        // Arrange
        String username = "";
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertTrue(!result.getLessonCompleted(), "Expected failure when username is blank");

        // Also verify no DB interaction in this validation failure path
        Mockito.verifyNoInteractions(connection);
    }
}
