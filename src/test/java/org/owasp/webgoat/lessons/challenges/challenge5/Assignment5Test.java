package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta unit tests for Assignment5 focusing only on the changed behavior:
 * - SQL is now parameterized using placeholders.
 * - Parameters are bound via PreparedStatement#setString in the correct order.
 * - Functional behavior for valid/invalid credentials is preserved.
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
    dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    flags = org.mockito.Mockito.mock(Flags.class);
    assignment5 = new Assignment5(dataSource, flags);

    connection = org.mockito.Mockito.mock(Connection.class);
    preparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
    resultSet = org.mockito.Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }

  @Test
  void login_usesParameterizedQuery_andBindsParametersInOrder() throws Exception {
    String username = "Larry";
    String password = "secret-password";

    // Arrange valid user so that query is executed
    when(resultSet.next()).thenReturn(false);

    assignment5.login(username, password);

    // Capture SQL used to prepare the statement
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String actualSql = sqlCaptor.getValue();

    // Assert SQL uses placeholders instead of direct concatenation
    // The exact string is known from the fixed implementation
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        actualSql,
        "SQL query should use parameter placeholders for username and password");

    // Assert parameters are bound correctly and in the right order
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
  }

  @Test
  void login_returnsSuccessWhenCredentialsMatch() throws Exception {
    String username = "Larry";
    String password = "secret-password";

    when(resultSet.next()).thenReturn(true);
    AttackResult expectedSuccess =
        org.owasp.webgoat.container.assignments.AttackResultBuilder
            .success(assignment5)
            .build();
    // We cannot easily reconstruct the exact internal AttackResult content,
    // but we can assert on the success flag.
    AttackResult result = assignment5.login(username, password);

    // Assert the result represents a successful attack resolution
    // (behavior preserved while using parameterized SQL)
    org.junit.jupiter.api.Assertions.assertTrue(
        result.getLessonCompleted(),
        "Login should report success when the query returns a row");
  }

  @Test
  void login_returnsFailureWhenCredentialsDoNotMatch() throws Exception {
    String username = "Larry";
    String password = "wrong-password";

    when(resultSet.next()).thenReturn(false);

    AttackResult result = assignment5.login(username, password);

    org.junit.jupiter.api.Assertions.assertFalse(
        result.getLessonCompleted(),
        "Login should report failure when the query does not return a row");
  }

  @Test
  void login_shortCircuitsOnEmptyInput_beforeHittingDatabase() throws Exception {
    // This test ensures behavior for empty input is preserved
    String username = "";
    String password = "some-pass";

    AttackResult result = assignment5.login(username, password);

    org.junit.jupiter.api.Assertions.assertFalse(
        result.getLessonCompleted(),
        "Empty username should fail before DB interaction");
    // Verify connection was never requested (no SQL executed)
    org.mockito.Mockito.verifyNoInteractions(connection);
  }
}
