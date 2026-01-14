package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the SQL injection fix:
 * - Ensure a parameterized PreparedStatement is used
 * - Ensure user input is bound via setString rather than concatenated into SQL
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
    dataSource = Mockito.mock(LessonDataSource.class);
    flags = Mockito.mock(Flags.class);
    connection = Mockito.mock(Connection.class);
    preparedStatement = Mockito.mock(PreparedStatement.class);
    resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    assignment5 = new Assignment5(dataSource, flags);
  }

  @Test
  void login_usesParameterizedQueryAndBindsUserInputs() throws Exception {
    String username = "Larry";
    String password = "somePassword";
    AttackResult result = assignment5.login(username, password);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    // Assert the SQL uses parameter placeholders instead of concatenating user input
    assertTrue(sql.contains("where userid = ? and password = ?"),
        "SQL must use parameter placeholders for userid and password");
    assertTrue(!sql.contains(username) && !sql.contains(password),
        "SQL string must not contain raw user values");

    // Verify that user input is passed via setString on the PreparedStatement
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // No other unexpected parameter bindings
    verify(preparedStatement, never()).setString(3, anyString());

    // Behavior remains consistent (no SQL error)
    assertEquals("challenge.close", result.getFeedback(), "Expected normal failure feedback");
  }

  @Test
  void login_rejectsSqlInjectionPayloadStillUsingParameterizedQuery() throws Exception {
    String maliciousUsername = "Larry' OR '1'='1";
    String maliciousPassword = "anything";

    AttackResult result = assignment5.login(maliciousUsername, maliciousPassword);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    // Ensure the SQL itself is unchanged and still using parameters
    assertTrue(sql.contains("where userid = ? and password = ?"),
        "SQL must remain parameterized even under injection-like input");

    // Ensure the malicious payload is only present as bound parameters
    verify(preparedStatement).setString(1, maliciousUsername);
    verify(preparedStatement).setString(2, maliciousPassword);

    // The login must still fail (no shortcut success due to injection)
    assertEquals("challenge.close", result.getFeedback(), "Injection attempt must not succeed");
  }
}
