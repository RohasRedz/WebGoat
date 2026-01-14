/*
 * Delta tests for Assignment5 focusing on the change from string-concatenated SQL
 * to a parameterized PreparedStatement. These tests verify that user input is passed
 * as parameters rather than concatenated into the SQL string, mitigating SQL injection.
 */
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

public class Assignment5Test {

  private LessonDataSource lessonDataSource;
  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private Flags flags;
  private Assignment5 assignment5;

  @BeforeEach
  void setUp() throws Exception {
    lessonDataSource = mock(LessonDataSource.class);
    dataSource = mock(DataSource.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    flags = mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("flag-5");

    assignment5 = new Assignment5(lessonDataSource, flags);
  }

  @Test
  void login_usesParameterizedQueryAndBindsUserInputs() throws Exception {
    String username = "Larry";
    String password = "SecureP@ssw0rd";

    AttackResult result = assignment5.login(username, password);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();

    // Assert that the SQL contains parameter placeholders rather than concatenated values
    assertTrue(
        usedSql.toLowerCase().contains("userid = ?")
            && usedSql.toLowerCase().contains("password = ?"),
        "SQL must use parameter placeholders instead of concatenated user input");

    // Assert that the user input values are passed via setString on the PreparedStatement
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // Behaviorally, a successful match should still return a successful AttackResult
    assertTrue(result.getSuccess(), "Successful login with correct credentials should still succeed");
  }

  @Test
  void login_doesNotEmbedUserInputDirectlyInSqlString() throws Exception {
    String maliciousUsername = "Larry' OR '1'='1";
    String maliciousPassword = "anything";

    assignment5.login(maliciousUsername, maliciousPassword);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();

    // Ensure raw user-controlled strings are not present verbatim in the SQL text
    assertTrue(
        !usedSql.contains(maliciousUsername) && !usedSql.contains(maliciousPassword),
        "User input must not be concatenated directly into the SQL string");
  }
}
