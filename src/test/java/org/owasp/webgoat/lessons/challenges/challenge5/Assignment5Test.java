package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

/**
 * Delta tests for Assignment5 focusing on the updated parameterized SQL behavior.
 *
 * Derived test path (per requirements):
 *   src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
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
  void login_usesParameterizedQueryAndBindsUserInputs() throws Exception {
    when(resultSet.next()).thenReturn(true);

    String username = "Larry";
    String password = "secretPassword";
    AttackResult result = assignment5.login(username, password);

    // Verify that a parameterized SQL statement is used (with ? placeholders)
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection, times(1)).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();
    // The fixed code must not concatenate the username/password directly
    // and must use placeholders instead.
    // Note: We assert on the expected query string introduced by the fix.
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        usedSql,
        "SQL must use parameter placeholders instead of string concatenation");

    // Verify that both parameters are bound in the correct order
    verify(preparedStatement, times(1)).setString(1, username);
    verify(preparedStatement, times(1)).setString(2, password);

    // Ensure normal success behavior is preserved
    assertSame(
        AttackResult.class, result.getClass(), "login should still return an AttackResult");
  }

  @Test
  void login_doesNotAllowSqlInjectionViaUserInput() throws Exception {
    // Simulate that the query returns no row, even when injection payload is provided.
    when(resultSet.next()).thenReturn(false);

    String maliciousUsername = "Larry' OR '1'='1";
    String maliciousPassword = "anything' OR '1'='1";

    AttackResult result = assignment5.login(maliciousUsername, maliciousPassword);

    // Capture SQL used by the method
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection, times(1)).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();

    // Because we now use parameterized queries, the raw payload should NEVER be concatenated
    // directly into the SQL string.
    // We assert the exact safe query introduced by the fix.
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        usedSql,
        "SQL must not contain raw user input even if it is malicious");

    // Verify that the malicious values are safely bound as parameters (not concatenated)
    verify(preparedStatement, times(1)).setString(1, maliciousUsername);
    verify(preparedStatement, times(1)).setString(2, maliciousPassword);
  }
}
