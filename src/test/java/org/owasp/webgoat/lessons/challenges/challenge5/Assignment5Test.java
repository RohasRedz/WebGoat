package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.anyString;
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
 * - Ensure a PreparedStatement with parameter placeholders is used.
 * - Ensure user input is bound via setString rather than concatenated into the SQL.
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
    dataSource = Mockito.mock(LessonDataSource.class);
    flags = Mockito.mock(Flags.class);
    assignment5 = new Assignment5(dataSource, flags);

    connection = Mockito.mock(Connection.class);
    preparedStatement = Mockito.mock(PreparedStatement.class);
    resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }

  @Test
  void login_usesParameterizedQueryAndBindsUserInput() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "p@ssw0rd";
    when(resultSet.next()).thenReturn(true);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert: PreparedStatement is created with parameter placeholders (no direct concatenation)
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();
    // The exact SQL text may evolve, but it must use placeholders for both userid and password.
    org.junit.jupiter.api.Assertions.assertTrue(
        usedSql.toLowerCase().contains("userid = ?") && usedSql.toLowerCase().contains("password = ?"),
        "SQL should use parameter placeholders instead of concatenating user input");

    // Assert: user input is provided via setString bindings (defense against injection)
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // Sanity: successful login still works for normal credentials
    org.junit.jupiter.api.Assertions.assertTrue(result.isSuccess());
  }

  @Test
  void login_rejectsSqlInjectionPayloadInsteadOfTreatingItAsSql() throws Exception {
    // Arrange: attacker attempts to inject via password parameter
    String username = "Larry";
    String maliciousPassword = "' OR '1'='1";
    when(resultSet.next()).thenReturn(false); // no row should be matched

    // Act
    AttackResult result = assignment5.login(username, maliciousPassword);

    // Assert: parameters are bound exactly as provided (no SQL-level effect)
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, maliciousPassword);

    // The attack should not succeed; the lesson should treat it as an invalid login
    org.junit.jupiter.api.Assertions.assertFalse(result.isSuccess());
  }
}
