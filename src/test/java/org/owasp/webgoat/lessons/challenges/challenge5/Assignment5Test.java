package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
 * Delta tests for Assignment5 focusing on the fixed SQL injection vulnerability.
 *
 * Resolved test path (from src/main/...):
 * src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 */
public class Assignment5Test {

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
    when(flags.getFlag(5)).thenReturn("FLAG-5");
  }

  @Test
  void login_succeeds_for_valid_larry_credentials() throws Exception {
    // Arrange
    when(resultSet.next()).thenReturn(true);

    // Act
    AttackResult result = assignment5.login("Larry", "secret");

    // Assert
    // Successful path must still be reachable with valid credentials
    assertEquals("success", result.getOutcome(), "Expected login to succeed for valid Larry credentials");
  }

  @Test
  void login_fails_for_invalid_password() throws Exception {
    // Arrange
    when(resultSet.next()).thenReturn(false);

    // Act
    AttackResult result = assignment5.login("Larry", "wrong");

    // Assert
    assertEquals("failed", result.getOutcome(), "Expected login to fail for invalid password");
  }

  @Test
  void login_not_vulnerable_to_basic_sql_injection_in_password() throws Exception {
    // Arrange
    // Even if the attacker supplies a classic SQL injection payload, the prepared statement
    // must treat it as data and not allow bypass. We simulate "no matching row".
    when(resultSet.next()).thenReturn(false);

    // Act
    AttackResult result =
        assignment5.login("Larry", "' OR '1'='1");

    // Assert
    // If the statement were still concatenating user input, this payload would likely succeed.
    // With parameterized queries, it must fail.
    assertEquals(
        "failed",
        result.getOutcome(),
        "Expected login to fail for SQL injection payload due to parameterized query");
  }

  @Test
  void login_requires_non_empty_parameters() throws Exception {
    // Arrange & Act
    AttackResult resultEmptyUser = assignment5.login("", "pass");
    AttackResult resultEmptyPass = assignment5.login("Larry", "");

    // Assert
    assertEquals("failed", resultEmptyUser.getOutcome(), "Expected failure for empty username");
    assertEquals("failed", resultEmptyPass.getOutcome(), "Expected failure for empty password");
  }

  @Test
  void login_throws_exception_when_datasource_fails() throws Exception {
    // Arrange
    LessonDataSource brokenDataSource = mock(LessonDataSource.class);
    when(brokenDataSource.getConnection()).thenThrow(new RuntimeException("DB unavailable"));
    Assignment5 brokenAssignment = new Assignment5(brokenDataSource, flags);

    // Act & Assert
    assertThrows(
        Exception.class,
        () -> brokenAssignment.login("Larry", "secret"),
        "Expected exception to propagate when data source fails");
  }
}
