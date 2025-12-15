package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * 1) SQL must use a parameterized PreparedStatement instead of string concatenation.
 * 2) Successful login for valid credentials (Larry + correct password) still succeeds.
 * 3) Invalid credentials still fail.
 *
 * NOTE: These tests are intentionally narrow and only validate the regression surface
 * of the vulnerability fix, not the entire class behavior.
 */
class Assignment5Test {

  @Test
  @DisplayName("login() should use parameterized PreparedStatement and NOT concatenate user input into SQL")
  void loginShouldUseParameterizedPreparedStatement() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false); // behavior not important for this test

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "P@ss' OR '1'='1"; // Attempted injection-like payload

    // Act
    assignment5.login(username, password);

    // Assert
    // Verify that a PreparedStatement is created once
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());

    String sqlUsed = sqlCaptor.getValue();
    // Ensure the SQL contains placeholders instead of raw user input
    // 1) It should contain parameter placeholders '?'
    // 2) It must NOT directly contain the username or password values
    //    This ensures no string concatenation with user input is used in the query.
    org.assertj.core.api.Assertions.assertThat(sqlUsed)
        .as("SQL should use placeholders instead of embedding user input directly")
        .contains("userid = ?")
        .contains("password = ?");

    org.assertj.core.api.Assertions.assertThat(sqlUsed)
        .as("SQL should not contain raw username")
        .doesNotContain(username);

    org.assertj.core.api.Assertions.assertThat(sqlUsed)
        .as("SQL should not contain raw password (even if malicious)")
        .doesNotContain(password);

    // Verify that parameters were bound via setString()
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
  }

  @Test
  @DisplayName("login() should return success AttackResult for valid Larry credentials when DB returns a row")
  void loginShouldSucceedForValidLarryCredentialsWhenRowExists() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true); // simulate a matching row
    when(flags.getFlag(5)).thenReturn("FLAG-5"); // example flag value

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "correct-password";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // We can't easily inspect internal fields of AttackResult without knowing its API,
    // but typically AttackResult has a boolean or status we can inspect.
    // Here we rely on its toString() or getLessonName()/getFeedback() if available.
    // TODO: If AttackResult exposes an explicit success flag, assert that instead of toString().
    org.assertj.core.api.Assertions.assertThat(result)
        .as("Result should not be null")
        .isNotNull();

    // We assert via its string representation as a fallback to avoid assumptions about internals.
    String resultString = result.toString();
    org.assertj.core.api.Assertions.assertThat(resultString)
        .as("Result should indicate challenge is solved or success after valid credentials")
        .containsIgnoringCase("success")
        .containsIgnoringCase("challenge.solved");

    // Additionally, verify that the flag was requested
    verify(flags).getFlag(5);
  }

  @Test
  @DisplayName("login() should return failure AttackResult when DB does not return a row for Larry")
  void loginShouldFailForLarryWhenNoRowExists() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false); // simulate no matching row

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "wrong-password";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    org.assertj.core.api.Assertions.assertThat(result)
        .as("Result should not be null")
        .isNotNull();

    String resultString = result.toString();
    org.assertj.core.api.Assertions.assertThat(resultString)
        .as("Result should indicate failure / challenge not solved")
        .doesNotContainIgnoringCase("challenge.solved");

    // No flag should be requested in the failure case
    verify(flags, never()).getFlag(anyInt());
  }

  @Test
  @DisplayName("login() should fail for non-Larry users even if the DB would return a row")
  void loginShouldFailForNonLarryUserRegardlessOfDb() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Mallory"; // not Larry
    String password = "any-password";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    org.assertj.core.api.Assertions.assertThat(result)
        .as("Result for non-Larry should not be null")
        .isNotNull();

    String resultString = result.toString();
    org.assertj.core.api.Assertions.assertThat(resultString)
        .as("Result should indicate user is not Larry and therefore fail")
        .containsIgnoringCase("user.not.larry");

    // Ensure DB is never touched for non-Larry user
    verifyNoInteractions(dataSource, flags);
  }

  // Optional helper to make AssertJ + JUnit 5 interplay clear (not strictly necessary)
  @SuppressWarnings("unused")
  private static void assertStatusEquals(String expected, AttackResult result) {
    // TODO: Replace this helper with explicit AttackResult API checks if available in the project.
    assertEquals(expected, result.toString());
  }
}
