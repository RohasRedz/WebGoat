package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta unit tests for Assignment5 focusing only on:
 * - Ensuring the SQL is no longer built via direct string concatenation with user input
 *   but instead uses parameterized PreparedStatement placeholders.
 * - Verifying that login behavior (success/failure) is preserved when using the
 *   parameterized query (for user 'Larry').
 *
 * NOTE:
 * - These tests are intentionally scoped to the changed behavior and do not
 *   re-test unrelated functionality.
 * - We use Mockito to mock JDBC and related infrastructure to avoid external systems.
 */
class Assignment5Test {

  // Constants taken from the fixed implementation for stronger coupling to the changed behavior
  private static final String EXPECTED_SQL =
      "select password from challenge_users where userid = ? and password = ?";

  @Test
  @DisplayName("login should use parameterized SQL with placeholders and bind user input via setString")
  void login_shouldUseParameterizedQueryAndNotConcatenateUserInput() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    String username = "Larry";
    String password = "somePassword";

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false); // We only care about the SQL and bindings here

    // Act
    assignment5.login(username, password);

    // Assert
    // 1) Ensure the SQL passed to prepareStatement is the parameterized one with '?' placeholders
    verify(connection, times(1)).prepareStatement(eq(EXPECTED_SQL));

    // 2) Ensure that user input is bound via setString and not concatenated into the SQL string
    verify(preparedStatement, times(1)).setString(1, username);
    verify(preparedStatement, times(1)).setString(2, password);

    // 3) Ensure the query is executed
    verify(preparedStatement, times(1)).executeQuery();

    // No verification of string concatenation is needed: if concatenation were used,
    // the SQL string captured above would differ from EXPECTED_SQL and this test
    // would fail. This directly validates that the vulnerability is fixed.
  }

  @Test
  @DisplayName("login should succeed for Larry with correct password when query is securely parameterized")
  void login_shouldSucceedForLarryWithValidCredentials() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    String username = "Larry";
    String password = "correctPassword";

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(EXPECTED_SQL)).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true); // Simulate a successful match in DB

    String expectedFlag = "FLAG-5";
    when(flags.getFlag(5)).thenReturn(expectedFlag);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // We only assert HTTP/lesson semantics that should remain unchanged
    // (i.e., success path still works with parameterized SQL).
    // WebGoat's AttackResult uses isSuccess() to indicate success.
    assertEquals(true, result.getLessonCompleted(), "Login should complete the lesson for valid Larry credentials");
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(flags, times(1)).getFlag(5);
  }

  @Test
  @DisplayName("login should fail for Larry with invalid credentials even with parameterized query")
  void login_shouldFailForLarryWithInvalidCredentials() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    String username = "Larry";
    String wrongPassword = "wrongPassword";

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(EXPECTED_SQL)).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false); // Simulate no matching row

    // Act
    AttackResult result = assignment5.login(username, wrongPassword);

    // Assert
    // Behavior should remain: invalid password should not solve the challenge,
    // and the parameterized query should still be used.
    assertEquals(false, result.getLessonCompleted(), "Login should not complete the lesson for invalid credentials");
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, wrongPassword);
  }

  @Test
  @DisplayName("login should reject non-Larry user before reaching SQL layer (unchanged behavior, protects from injection attempts)")
  void login_shouldRejectNonLarryUserBeforeSqlExecution() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Mallory";
    String somePassword = "doesNotMatter";

    // Act
    AttackResult result = assignment5.login(username, somePassword);

    // Assert
    // This verifies an important guard that helps protect against injection:
    // non-Larry users are rejected early and never reach the SQL call.
    assertEquals(false, result.getLessonCompleted(), "Non-Larry users must be rejected before SQL execution");

    // Ensure dataSource is never called when username is not Larry
    verifyNoInteractions(dataSource);
  }

  // Sanity check aligning with existing behavior: the presence checks using StringUtils are unchanged.
  @Test
  @DisplayName("login should fail when username or password is empty (precondition unchanged)")
  void login_shouldFailWhenUsernameOrPasswordEmpty() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    AttackResult result1 = assignment5.login("", "password");
    AttackResult result2 = assignment5.login("Larry", "");

    assertEquals(false, result1.getLessonCompleted(), "Empty username should fail the login");
    assertEquals(false, result2.getLessonCompleted(), "Empty password should fail the login");

    // Again, ensure that for invalid input we don't even touch the DB
    verifyNoInteractions(dataSource);
  }
}
