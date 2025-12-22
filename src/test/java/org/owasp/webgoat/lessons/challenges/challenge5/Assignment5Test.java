package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta unit tests for Assignment5 focusing only on the changed SQL behavior:
 *  - Verifies that username and password are bound as PreparedStatement parameters (no concatenation).
 *  - Verifies success and failure paths still behave correctly with parameterized queries.
 *
 * These tests do NOT attempt to reproduce the old vulnerable behavior; instead they assert
 * the presence and correct use of PreparedStatement parameters.
 */
class Assignment5Test {

  @Test
  @DisplayName("login() uses PreparedStatement parameters for userid and password and returns success when credentials are valid")
  void loginUsesPreparedStatementParametersAndSucceedsForValidUser() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(lessonDataSource.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String username = "Larry";
    String password = "secret";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Ensure parameters are bound (this is the security-relevant behavior)
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();

    // Ensure success path is preserved
    // We cannot assert message keys directly without full WebGoat infrastructure,
    // but we can assert that the result is a success.
    // AttackResult does not expose a simple "isSuccess" in this context, so we
    // use its toString representation as a minimal, non-invasive check.
    // TODO: Replace with a direct success assertion if AttackResult has such an API.
    String resultString = result.toString();
    // Basic sanity check: the result should mention the flag we passed in
    // which indicates that the success path was followed.
    // This avoids asserting internal, non-public APIs.
    org.junit.jupiter.api.Assertions.assertTrue(
        resultString.contains("FLAG-5"),
        "Expected success path to include flag from Flags.getFlag(5)");

    // Additionally, capture that the correct SQL with placeholders is used
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        sqlCaptor.getValue());
  }

  @Test
  @DisplayName("login() uses PreparedStatement parameters and fails for invalid credentials while still using parameterized query")
  void loginUsesPreparedStatementParametersAndFailsForInvalidCredentials() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(lessonDataSource.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String username = "Larry"; // valid username constraint
    String password = "wrong-password"; // invalid password

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Verify the security-relevant behavior: still using parameters and not concatenating input
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        sqlCaptor.getValue());

    // Check that the result indicates a failure path behaviorally distinct from success
    String resultString = result.toString();
    org.junit.jupiter.api.Assertions.assertFalse(
        resultString.contains("FLAG-5"),
        "Failure path for invalid credentials must not expose success flag");
  }

  @Test
  @DisplayName("login() does not hit database when username is not Larry (guard behavior unchanged and independent from SQL fix)")
  void loginDoesNotHitDatabaseWhenUserIsNotLarry() throws Exception {
    // This test guards that the pre-existing authorization check still prevents DB calls
    // and therefore cannot be bypassed even with the new parameterized SQL query.

    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    DataSource dataSource = mock(DataSource.class);
    when(lessonDataSource.getDataSource()).thenReturn(dataSource);

    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String username = "Mallory"; // not Larry
    String password = "whatever";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Database must never be touched when username is not Larry
    verifyNoInteractions(dataSource);

    String resultString = result.toString();
    org.junit.jupiter.api.Assertions.assertFalse(
        resultString.contains("FLAG-5"),
        "Non-Larry user should not reach success path or see any flag");
  }
}
