package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionChallenge focusing on:
 * - Using PreparedStatement with parameter binding for the username lookup.
 * - Ensuring no direct concatenation of user input into the query.
 */
class SqlInjectionChallengeTest {

  @Test
  void registerNewUser_usesPreparedStatementForUserLookup() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    Connection connection = mock(Connection.class);
    PreparedStatement checkUserStmt = mock(PreparedStatement.class);
    PreparedStatement insertStmt = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString()))
        .thenReturn(checkUserStmt)
        .thenReturn(insertStmt);
    when(checkUserStmt.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    String username = "newuser";
    String email = "user@example.com";
    String password = "StrongPass!";

    // Act
    AttackResult result = challenge.registerNewUser(username, email, password);

    // Assert
    verify(connection, times(2)).prepareStatement(anyString());

    // Verify check-user query is parameterized and does not contain the username literal
    verify(checkUserStmt).setString(1, username);
    verify(checkUserStmt).executeQuery();

    // Also confirm that the insert prepared statement binds all parameters correctly
    verify(insertStmt).setString(1, username);
    verify(insertStmt).setString(2, email);
    verify(insertStmt).setString(3, password);

    assertThat(result.getLessonCompleted()).isNull(); // informational message, not completion
  }

  @Test
  void registerNewUser_failsOnExistingUser() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    Connection connection = mock(Connection.class);
    PreparedStatement checkUserStmt = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(checkUserStmt);
    when(checkUserStmt.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);

    String username = "existing";
    String email = "existing@example.com";
    String password = "pass";

    // Act
    AttackResult result = challenge.registerNewUser(username, email, password);

    // Assert
    verify(checkUserStmt).setString(1, username);
    assertThat(result.getLessonCompleted()).isFalse();
  }
}
