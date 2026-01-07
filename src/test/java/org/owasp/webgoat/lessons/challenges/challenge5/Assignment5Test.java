package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta test for Assignment5 focusing only on the changed SQL behavior:
 * - Ensures the PreparedStatement uses parameter placeholders instead of string concatenation.
 * - Ensures bound parameters match the provided username and password.
 * - Verifies that a successful query still returns a success AttackResult (behavior preserved).
 */
public class Assignment5Test {

  private LessonDataSource dataSource;
  private Flags flags;
  private Assignment5 assignment;

  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = mock(LessonDataSource.class);
    flags = mock(Flags.class);
    assignment = new Assignment5(dataSource, flags);

    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }

  @Test
  void login_usesParameterizedPreparedStatement_andReturnsSuccessForValidCredentials()
      throws Exception {
    // Arrange
    String username = "Larry";
    String password = "secretPassword";

    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // Act
    AttackResult result = assignment.login(username, password);

    // Assert: verify the exact SQL now uses parameter placeholders (no user data concatenation)
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");

    // Assert: verify parameters are bound correctly (defence against SQL injection)
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // Assert: behavior preserved – successful query yields success result
    // AttackResult is a framework type; we only check that it reports success,
    // not its internal structure.
    assertEquals(
        true, result.isLessonCompleted(), "Expected login to succeed for valid credentials");
  }

  @Test
  void login_usesParameterizedPreparedStatement_andFailsWhenResultSetEmpty() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "wrongPassword";

    when(resultSet.next()).thenReturn(false);

    // Act
    AttackResult result = assignment.login(username, password);

    // Assert: SQL shape is still parameterized
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");

    // Assert: parameters still bound correctly even for failing login
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // Assert: behavior preserved – failed query yields failed result
    assertEquals(
        false, result.isLessonCompleted(), "Expected login to fail when no rows are returned");
  }
}
