package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.owasp.webgoat.container.assignments.AttackResult.Type.FAILURE;
import static org.owasp.webgoat.container.assignments.AttackResult.Type.INFORMATION;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionChallenge focusing on the updated user-existence check
 * that now uses a parameterized PreparedStatement.
 *
 * Verifies:
 * - Existing user path returns the appropriate failure result.
 * - Non-existing user path results in an insert and informational success.
 */
class SqlInjectionChallengeTest {

  @Test
  void registerNewUser_existingUser_returnsFailure() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    Connection connection = mock(Connection.class);
    PreparedStatement checkStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    PreparedStatement insertStatement = mock(PreparedStatement.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
        .thenReturn(checkStatement);
    when(checkStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);

    // insert statement should not be executed when user exists
    when(connection.prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)"))
        .thenReturn(insertStatement);

    AttackResult result =
        challenge.registerNewUser("existingUser", "user@example.com", "password");

    assertEquals(FAILURE, result.getType(), "Existing user registration should fail");
    verify(checkStatement).setString(1, "existingUser");
    verify(insertStatement, never()).execute();
  }

  @Test
  void registerNewUser_newUser_insertsAndReturnsInformation() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    Connection connection = mock(Connection.class);
    PreparedStatement checkStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    PreparedStatement insertStatement = mock(PreparedStatement.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
        .thenReturn(checkStatement);
    when(checkStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    when(connection.prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)"))
        .thenReturn(insertStatement);

    AttackResult result =
        challenge.registerNewUser("newUser", "user@example.com", "password");

    assertEquals(
        INFORMATION, result.getType(), "New user registration should return informational success");
    verify(checkStatement).setString(1, "newUser");
    verify(insertStatement).setString(1, "newUser");
    verify(insertStatement).setString(2, "user@example.com");
    verify(insertStatement).setString(3, "password");
    verify(insertStatement).execute();
  }
}
