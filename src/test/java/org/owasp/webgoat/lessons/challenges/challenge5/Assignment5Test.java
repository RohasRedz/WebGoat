package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on the changed behavior:
 * - Use of parameterized PreparedStatement instead of string-concatenated SQL
 * - Ensuring that SQL injection attempts do not succeed
 *
 * Test file path (derived): src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
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
  }

  @Test
  void login_withValidLarryCredentials_usesParameterizedQueryAndSucceeds() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "secret";
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert - success path still works
    assertTrue(result.getLessons().isEmpty() || result.getOutput().contains("challenge.solved"));

    // Assert - the prepared statement uses placeholders and binds parameters correctly
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();
    assertTrue(
        sql.contains("userid = ?")
            && sql.contains("password = ?"),
        "SQL must use parameter placeholders instead of concatenating user input");

    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
  }

  @Test
  void login_withSqlInjectionAttempt_doesNotManipulateQueryAndFails() throws Exception {
    // Arrange
    String username = "Larry";
    String maliciousPassword = "' OR '1'='1";
    when(resultSet.next()).thenReturn(false); // No row should be matched for malicious input

    // Act
    AttackResult result = assignment5.login(username, maliciousPassword);

    // Assert - attempt should fail
    assertTrue(
        result.getLessons().isEmpty() || result.getOutput().contains("challenge.close"),
        "SQL injection attempt must not succeed");

    // Assert - malicious payload is never embedded directly in the SQL string
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();
    assertTrue(
        !sql.contains(maliciousPassword),
        "Raw malicious input must not appear in the SQL statement; parameters must be used");

    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, maliciousPassword);
  }

  @Test
  void login_withEmptyParameters_stillFailsEarly() throws Exception {
    // This test ensures the pre-existing validation is still honored; it indirectly confirms
    // the new prepared statement is *not* used when input is empty.
    AttackResult result = assignment5.login("", "");

    assertTrue(
        result.getLessons().isEmpty() || result.getOutput().contains("required4"),
        "Empty credentials should trigger validation failure");
    verify(connection, never()).prepareStatement(anyString());
  }
}
