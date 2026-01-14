package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

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
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * use of parameterized PreparedStatement instead of string-concatenated SQL.
 *
 * Derived test path from:
 * src/main/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java
 *  src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query and solves challenge on correct credentials")
  void login_usesParameterizedQuery_andReturnsSuccessOnValidCredentials() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    // Simulate a successful lookup
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "correct-password";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert: verify SQL text uses placeholders (no concatenation of user input)
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());

    String sql = sqlCaptor.getValue();
    // The fixed code must use ? placeholders for both userid and password
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        sql,
        "SQL must be parameterized with placeholders and not built via string concatenation");

    // Assert: verify user inputs are bound as parameters, not concatenated into SQL
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verifyNoMoreInteractions(preparedStatement);

    // Assert: behavior  success path is preserved
    AttackResult expected =
        success(assignment5).feedback("challenge.solved").feedbackArgs("FLAG-5").build();
    assertEquals(
        expected.getLessonCompleted(),
        result.getLessonCompleted(),
        "Successful query should mark challenge as solved");
  }

  @Test
  @DisplayName("login fails when credentials do not match, still using parameterized query")
  void login_usesParameterizedQuery_andReturnsFailureOnInvalidCredentials() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    // Simulate no matching row
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "wrong-password";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert: verify SQL still uses placeholders
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());

    String sql = sqlCaptor.getValue();
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        sql,
        "SQL must remain parameterized even when authentication fails");

    // Assert: parameters are bound correctly for invalid credentials as well
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // Assert: behavior  failure path is preserved
    AttackResult expected =
        failed(assignment5).feedback("challenge.close").build();
    assertEquals(
        expected.getLessonCompleted(),
        result.getLessonCompleted(),
        "Unsuccessful query should keep challenge unsolved");
  }
}
