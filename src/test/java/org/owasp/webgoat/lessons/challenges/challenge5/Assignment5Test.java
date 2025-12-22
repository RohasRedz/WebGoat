// Assumed package based on source path; adjust if actual package differs.
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
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * - SQL is now parameterized (no concatenation of user input).
 * - Correct login outcomes still work as before.
 */
class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query and returns success for valid Larry credentials")
  void login_usesParameterizedQuery_andAuthenticatesLarry() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    DataSource delegate = mock(DataSource.class);
    when(lessonDataSource.getConnection()).thenReturn(delegate.getConnection());
    Connection connection = mock(Connection.class);
    when(delegate.getConnection()).thenReturn(connection);

    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

    ResultSet resultSet = mock(ResultSet.class);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true); // simulate successful authentication

    Flags flags = mock(Flags.class);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String username = "Larry";
    String password = "password123";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // 1) Ensure a parameterized query is used (no concatenation in SQL)
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();

    // Verify that the SQL contains placeholders and not the raw user input
    // This asserts that the injection vector (concatenating user input) is no longer present.
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        usedSql,
        "SQL query must use placeholders and not string concatenation with user input");

    // 2) Ensure user input is bound via parameters, preserving login behavior
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();

    // 3) Ensure the successful branch is still taken for valid credentials
    // We only assert that the result is a success; the internal message content is tested indirectly.
    // AttackResult in WebGoat typically exposes success status via isSuccess().
    // If this accessor differs, adjust accordingly.
    assertEquals(
        true,
        result.getLessonCompleted(),
        "Login with correct Larry credentials should still succeed after the fix");
  }

  @Test
  @DisplayName("login fails when authentication fails while still using parameterized query")
  void login_failsAuthentication_withParameterizedQuery() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    DataSource delegate = mock(DataSource.class);
    when(lessonDataSource.getConnection()).thenReturn(delegate.getConnection());
    Connection connection = mock(Connection.class);
    when(delegate.getConnection()).thenReturn(connection);

    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

    ResultSet resultSet = mock(ResultSet.class);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false); // simulate failed authentication

    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String username = "Larry";
    String password = "wrongPassword";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Still expect the parameterized query
    verify(connection)
        .prepareStatement(
            eq("select password from challenge_users where userid = ? and password = ?"));
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();

    // Authentication fails but without exposing any SQL injection vector
    assertEquals(
        false,
        result.getLessonCompleted(),
        "Login with invalid credentials should fail while still using a safe parameterized query");
  }

  @Test
  @DisplayName("login is not vulnerable to simple SQL injection payload in username")
  void login_preventsSqlInjectionViaParameterizedQuery() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    DataSource delegate = mock(DataSource.class);
    when(lessonDataSource.getConnection()).thenReturn(delegate.getConnection());
    Connection connection = mock(Connection.class);
    when(delegate.getConnection()).thenReturn(connection);

    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

    ResultSet resultSet = mock(ResultSet.class);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    // Even if the payload is malicious, DB must treat it as data; we simulate no match
    when(resultSet.next()).thenReturn(false);

    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String maliciousUsername = "Larry' OR '1'='1";
    String password = "anything";

    // Act
    AttackResult result = assignment5.login(maliciousUsername, password);

    // Assert
    // Ensure that the malicious input is not embedded into the raw SQL, i.e., placeholders are used
    verify(connection)
        .prepareStatement(
            eq("select password from challenge_users where userid = ? and password = ?"));

    // The malicious payload is passed as a bound parameter (safe)
    verify(preparedStatement).setString(1, maliciousUsername);
    verify(preparedStatement).setString(2, password);

    // Authentication should fail; injection should not grant access
    assertEquals(
        false,
        result.getLessonCompleted(),
        "SQL injection payload in username must not bypass authentication after the fix");
  }
}
