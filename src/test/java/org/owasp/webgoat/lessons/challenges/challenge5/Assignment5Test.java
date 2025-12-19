// Assuming package based on source path; adjust if actual package differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on changed behavior:
 * - The SQL query now uses PreparedStatement with parameter placeholders instead of string concatenation.
 *
 * Security expectations:
 * - User input is bound via PreparedStatement parameters.
 * - Raw user-controlled values do not appear concatenated in the SQL string.
 */
class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized PreparedStatement and does not concatenate user input in SQL")
  void login_usesPreparedStatementWithParameters() throws Exception {
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(false);
    Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

    String username = "Larry";
    String password = "password' OR '1'='1";

    AttackResult result = assignment5.login(username, password);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());

    String usedSql = sqlCaptor.getValue();
    // Assert that the SQL uses placeholders, not raw concatenation
    assertTrue(usedSql.contains("userid = ?"), "SQL must use parameter placeholder for userid");
    assertTrue(usedSql.contains("password = ?"), "SQL must use parameter placeholder for password");
    assertFalse(usedSql.contains(username), "Username must not be concatenated directly into SQL");
    assertFalse(usedSql.contains(password), "Password must not be concatenated directly into SQL");

    // Assert that user input is bound via parameters on PreparedStatement
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
  }

  @Test
  @DisplayName("login does not treat SQL injection payloads as part of the SQL structure (parameter binding only)")
  void login_treatsInjectionPayloadAsData() throws Exception {
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(false);

    String username = "Larry";
    String maliciousPassword = "' OR '1'='1";

    assignment5.login(username, maliciousPassword);

    // Verify that even for an injection-like password, it is still set as a parameter,
    // not concatenated into the SQL string.
    verify(preparedStatement).setString(eq(2), eq(maliciousPassword));
  }
}
