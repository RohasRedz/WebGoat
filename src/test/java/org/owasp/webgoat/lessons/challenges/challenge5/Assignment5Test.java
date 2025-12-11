package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta unit tests for {@link Assignment5}.
 *
 * <p>These tests focus only on the behavior changed to fix the vulnerability:
 *
 * <ul>
 *   <li>Ensuring SQL is executed using a parameterized {@link PreparedStatement} with {@code ?}
 *       placeholders instead of direct string concatenation of user input.</li>
 *   <li>Ensuring the functional behavior of the login method remains unchanged for valid and
 *       invalid credentials.</li>
 * </ul>
 */
public class Assignment5Test {

  private LessonDataSource dataSource;
  private Flags flags;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  private Assignment5 assignment5;

  @BeforeEach
  void setUp() throws Exception {
    // Mocks for JDBC + collaborators
    dataSource = mock(LessonDataSource.class);
    flags = mock(Flags.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(flags.getFlag(anyInt())).thenReturn("FLAG-5");

    assignment5 = new Assignment5(dataSource, flags);
  }

  /**
   * Verifies that the SQL used by the login method is parameterized, uses {@code ?} placeholders,
   * and does not include direct concatenation of user input.
   *
   * <p>This ensures that:
   *
   * <ul>
   *   <li>The prepared statement template contains placeholders for parameters.</li>
   *   <li>User-supplied values are bound via {@link PreparedStatement#setString(int, String)}
   *       rather than concatenated into the SQL.</li>
   * </ul>
   */
  @Test
  void login_ShouldUseParameterizedPreparedStatement() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "superSecret";

    when(resultSet.next()).thenReturn(false);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    assertNotNull(result, "AttackResult must not be null");

    // Verify that a PreparedStatement was created with a parameterized SQL string
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();

    // The fixed code uses: "select password from challenge_users where userid = ? and password = ?"
    // We assert that it contains placeholders and not raw user values.
    // NOTE: We deliberately do not assert the full string to keep the test resilient to minor formatting changes.
    org.junit.jupiter.api.Assertions.assertTrue(
        usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
        "SQL must use parameter placeholders for userid and password");

    org.junit.jupiter.api.Assertions.assertFalse(
        usedSql.contains(username) || usedSql.contains(password),
        "SQL template must not contain raw user input; values must be bound as parameters");

    // Verify that user input is bound via setString on the PreparedStatement
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
  }

  /**
   * Ensures that, with correct credentials, the login method still returns a successful
   * {@link AttackResult} and uses the flag as before the fix.
   *
   * <p>This verifies that the functional behavior is preserved after switching to parameterized
   * queries.
   */
  @Test
  void login_WithValidLarryCredentials_ShouldReturnSuccess() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "correct-password";

    when(resultSet.next()).thenReturn(true); // simulate that credentials are valid
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    assertNotNull(result, "AttackResult must not be null");
    org.junit.jupiter.api.Assertions.assertTrue(
        result.isLessonCompleted(),
        "Login with valid Larry credentials should succeed and mark lesson as completed");
    // We only check that the message contains the flag to avoid depending on exact i18n keys
    org.junit.jupiter.api.Assertions.assertTrue(
        result.getOutput().contains("FLAG-5"),
        "Success result should reference the challenge flag");
  }

  /**
   * Ensures that, with incorrect password for Larry, the login method still returns a failed
   * {@link AttackResult}, preserving original semantics.
   */
  @Test
  void login_WithInvalidPassword_ShouldReturnFailure() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "wrong-password";

    when(resultSet.next()).thenReturn(false); // simulate invalid credentials

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    assertNotNull(result, "AttackResult must not be null");
    org.junit.jupiter.api.Assertions.assertFalse(
        result.isLessonCompleted(),
        "Login with invalid password should not mark lesson as completed");
  }

  /**
   * Ensures that attempts at SQL injection in the password parameter do not affect the query
   * semantics, i.e., they are treated as literal data and not as part of the SQL syntax.
   *
   * <p>This tests the secure behavior by verifying that the injected content is passed to
   * {@link PreparedStatement#setString(int, String)} rather than being concatenated into the SQL
   * string.
   */
  @Test
  void login_WithSqlInjectionAttemptInPassword_ShouldTreatInputAsData() throws Exception {
    // Arrange
    String username = "Larry";
    String injection = "' OR '1'='1";

    when(resultSet.next()).thenReturn(false);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

    // Act
    AttackResult result = assignment5.login(username, injection);

    // Assert
    assertNotNull(result, "AttackResult must not be null");

    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();

    // The SQL template must not contain the injected string
    org.junit.jupiter.api.Assertions.assertFalse(
        usedSql.contains(injection),
        "SQL template must not contain injected content; injection must be bound as a parameter value");

    // Injection must be bound as the second parameter value
    verify(preparedStatement).setString(eq(1), eq(username));
    verify(preparedStatement).setString(eq(2), eq(injection));

    // Behavior: query evaluates as normal credentials check; we simulate no matching row.
    assertEquals(
        false,
        result.isLessonCompleted(),
        "Login with SQL injection payload in password should not succeed");
  }
}
