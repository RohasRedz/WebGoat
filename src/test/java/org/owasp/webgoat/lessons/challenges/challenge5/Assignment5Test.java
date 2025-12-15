package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta unit tests for Assignment5 focusing only on:
 * - SQL query now using parameterized PreparedStatement instead of direct string concatenation.
 * - Ensuring user input is bound via parameters and that successful path still works.
 *
 * Note: We cannot easily inspect the internal SQL string from a PreparedStatement in a portable way,
 * but we can verify that parameters are bound correctly and that the logic path is preserved.
 */
class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment5;

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
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void loginShouldBindUserInputsAsParametersAndReturnSuccessForValidUser() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "correct-password";
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Ensure Assignment5 still uses Larry as the only valid user as per original behavior
        assertThat(result.getLessonCompleted())
                .as("Login should succeed when the query finds a matching row")
                .isTrue();

        // Verify that the query was created and user inputs were set via parameters
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void loginShouldFailWhenUserIsNotLarryBeforeReachingSqlLayer() throws Exception {
        // Arrange
        String username = "Mallory";
        String password = "anything";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Existing behavior: non-Larry user is rejected before SQL is executed
        assertThat(result.getLessonCompleted()).isFalse();
        verifyNoInteractions(connection);
    }

    @Test
    void loginShouldNotAttemptSqlWhenParametersAreEmpty() throws Exception {
        // Arrange
        String username = "";
        String password = "nonEmpty";

        // Sanity check to ensure StringUtils behavior matches expectations
        assertThat(StringUtils.hasText(username)).isFalse();

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertThat(result.getLessonCompleted()).isFalse();
        verifyNoInteractions(connection);
    }
}
