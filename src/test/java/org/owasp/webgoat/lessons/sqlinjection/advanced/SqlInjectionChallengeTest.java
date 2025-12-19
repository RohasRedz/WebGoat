// Assuming package based on source path; adjust if actual package differs.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionChallenge focusing only on changed behavior:
 * - The check-user query now uses PreparedStatement with parameterized SQL.
 *
 * Security expectations:
 * - The SQL string contains a placeholder for the username instead of concatenating it.
 * - The user-supplied username is bound via PreparedStatement#setString rather than
 *   being directly embedded in the SQL text.
 */
class SqlInjectionChallengeTest {

  @Test
  @DisplayName("registerNewUser uses parameterized PreparedStatement for user existence check")
  void registerNewUser_usesParameterizedCheckUserQuery() {
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement checkUserStatement = Mockito.mock(PreparedStatement.class);
    PreparedStatement insertStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    try {
      Mockito.when(dataSource.getConnection()).thenReturn(connection);
      // First prepareStatement call: check-user query
      Mockito.when(connection.prepareStatement(Mockito.anyString()))
          .thenReturn(checkUserStatement, insertStatement);
      Mockito.when(checkUserStatement.executeQuery()).thenReturn(resultSet);
      Mockito.when(resultSet.next()).thenReturn(false);

      String username = "user' OR '1'='1";
      String email = "user@example.com";
      String password = "pass123";

      AttackResult result =
          challenge.registerNewUser(username, email, password);

      // Capture SQL for the check-user query
      ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
      Mockito.verify(connection, Mockito.atLeastOnce()).prepareStatement(sqlCaptor.capture());

      String checkUserSql = sqlCaptor.getAllValues().get(0);

      assertTrue(
          checkUserSql.contains("where userid = ?"),
          "Check-user SQL must use parameter placeholder instead of concatenating username");
      assertFalse(
          checkUserSql.contains(username),
          "Username must not be concatenated directly into the check-user SQL");

      Mockito.verify(checkUserStatement).setString(1, username);

      // Ensure that the insert statement is still parameterized (existing secure behavior)
      Mockito.verify(insertStatement).setString(1, username);
      Mockito.verify(insertStatement).setString(2, email);
      Mockito.verify(insertStatement).setString(3, password);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("registerNewUser treats injection-like username as data via parameter binding")
  void registerNewUser_treatsInjectionPayloadAsData() {
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement checkUserStatement = Mockito.mock(PreparedStatement.class);
    PreparedStatement insertStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    try {
      Mockito.when(dataSource.getConnection()).thenReturn(connection);
      Mockito.when(connection.prepareStatement(Mockito.anyString()))
          .thenReturn(checkUserStatement, insertStatement);
      Mockito.when(checkUserStatement.executeQuery()).thenReturn(resultSet);
      Mockito.when(resultSet.next()).thenReturn(false);

      String maliciousUsername = "admin' --";
      String email = "attacker@example.com";
      String password = "pwd";

      challenge.registerNewUser(maliciousUsername, email, password);

      // Even for a malicious-looking username, it must be bound as a parameter,
      // not used to structurally modify the SQL command.
      Mockito.verify(checkUserStatement).setString(1, maliciousUsername);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
