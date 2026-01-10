package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the fixed behavior:
 * - SQL query must use parameterized PreparedStatement with bound parameters, not string concatenation.
 */
class Assignment5Test {

  @Test
  @DisplayName("login should use PreparedStatement with parameters for username and password")
  void login_usesParameterizedQuery() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("flag-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "safePassword";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();

    // Ensure that a successful result is still produced for valid credentials
    // (behavioral regression guard on the secured path).
    assert result.isLessonCompleted();
  }
}
