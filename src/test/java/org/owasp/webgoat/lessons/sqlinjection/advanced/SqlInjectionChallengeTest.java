package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionChallenge focusing on the parameterized checkUserQuery:
 * - ensure that user lookups use a PreparedStatement with a parameter placeholder.
 */
public class SqlInjectionChallengeTest {

  @Test
  @DisplayName("registerNewUser should use PreparedStatement with parameterized userid lookup")
  void registerNewUser_usesParameterizedUserCheckQuery() throws SQLException {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement checkUserStmt = Mockito.mock(PreparedStatement.class);
    PreparedStatement insertStmt = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    // First prepareStatement call: checkUserQuery
    Mockito
        .when(connection.prepareStatement(Mockito.startsWith("select userid from sql_challenge_users")))
        .thenReturn(checkUserStmt);
    // Second prepareStatement call: insert
    Mockito
        .when(connection.prepareStatement(Mockito.startsWith("INSERT INTO sql_challenge_users")))
        .thenReturn(insertStmt);

    Mockito.when(checkUserStmt.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(false); // user does not exist

    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    String username = "newuser";
    String email = "user@example.com";
    String password = "pwd";

    // Act
    AttackResult result = challenge.registerNewUser(username, email, password);

    // Assert: verify the checkUserQuery uses a ? placeholder and that username is bound
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
    String checkUserSql = sqlCaptor.getValue();
    assertTrue(
        checkUserSql.toLowerCase().contains("where userid = ?"),
        "User existence check query must be parameterized with a placeholder"
    );

    Mockito.verify(checkUserStmt).setString(1, username);

    // Behavior should remain: user is created when not existing
    assertTrue(result.getOutput().contains(username), "Result should mention the created user");
  }

  @Test
  @DisplayName("registerNewUser should still detect existing user using parameterized query")
  void registerNewUser_existingUserStillDetectedWithParameterizedQuery() throws SQLException {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement checkUserStmt = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito
        .when(connection.prepareStatement(Mockito.startsWith("select userid from sql_challenge_users")))
        .thenReturn(checkUserStmt);
    Mockito.when(checkUserStmt.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(true); // user already exists

    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    String username = "existing";
    String email = "user@example.com";
    String password = "pwd";

    // Act
    AttackResult result = challenge.registerNewUser(username, email, password);

    // Assert: parameter is still bound and behavior preserved
    Mockito.verify(checkUserStmt).setString(1, username);
    assertEquals(
        false,
        result.getLessonCompleted(),
        "Existing user should not lead to a completed lesson"
    );
  }
}
