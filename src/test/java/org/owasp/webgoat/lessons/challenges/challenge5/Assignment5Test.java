package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

/**
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * - SQL is now parameterized (no concatenation of user input).
 * - Successful login when correct user/password are passed through PreparedStatement.
 * - Failed login when credentials are incorrect.
 *
 * These tests mock JDBC interactions to verify behavior without depending on a real DB.
 */
class Assignment5Test {

  @Test
  @DisplayName("login should succeed for Larry with correct password using parameterized query")
  void loginSucceedsForLarryWithCorrectPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            eq("select password from challenge_users where userid = ? and password = ?")))
        .thenReturn(preparedStatement);
    // Simulate a matching row returned
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);

    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "secret");

    // Assert
    assertTrue(result.getLessonCompleted());
    assertEquals("challenge.solved", result.getFeedbackId());
  }

  @Test
  @DisplayName("login should fail for Larry with incorrect password using parameterized query")
  void loginFailsForLarryWithIncorrectPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            eq("select password from challenge_users where userid = ? and password = ?")))
        .thenReturn(preparedStatement);
    // Simulate no rows returned (wrong password)
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "wrong-password");

    // Assert
    assertTrue(!result.getLessonCompleted());
    assertEquals("challenge.close", result.getFeedbackId());
  }
}
