package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

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
 * Delta tests for Assignment5 focusing only on the SQL injection fix:
 * - ensure that login() uses a parameterized PreparedStatement
 *   with bound parameters instead of string concatenation.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login should authenticate using PreparedStatement with bound parameters")
  void login_usesPreparedStatementWithParameters() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito
        .when(connection.prepareStatement(Mockito.anyString()))
        .thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(true);

    Assignment5 assignment = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "CorrectPassword";

    // Act
    AttackResult result = assignment.login(username, password);

    // Assert: verify the SQL uses placeholders (no direct concatenation check, but pattern-based)
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();
    assertTrue(
        sql.toLowerCase().contains("where userid = ? and password = ?"),
        "SQL must use parameter placeholders instead of concatenated user input"
    );

    // Assert: verify user inputs are bound as parameters
    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, password);

    // Assert: still returns success on valid credentials
    assertTrue(result.getLessonCompleted(), "Login with correct credentials should succeed");
  }

  @Test
  @DisplayName("login should still fail when credentials do not match (prepared statement path)")
  void login_withInvalidCredentialsFailsButStillUsesParameters() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(false);

    Assignment5 assignment = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "WrongPassword";

    // Act
    AttackResult result = assignment.login(username, password);

    // Assert: parameters are still bound, even on failed login
    Mockito.verify(preparedStatement).setString(eq(1), eq(username));
    Mockito.verify(preparedStatement).setString(eq(2), eq(password));

    assertEquals(
        false,
        result.getLessonCompleted(),
        "Login with wrong credentials should not complete the lesson"
    );
  }
}
