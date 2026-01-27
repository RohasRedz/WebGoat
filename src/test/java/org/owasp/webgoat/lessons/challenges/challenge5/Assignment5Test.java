package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on the change from string-concatenated SQL
 * to a parameterized PreparedStatement. These tests verify that user input is no
 * longer directly embedded in the SQL string and that the query is still executed
 * correctly via bound parameters.
 *
 * NOTE: We do not execute against a real database; instead, we assert on interaction
 * with the JDBC API (prepareStatement and setString calls) and on returned AttackResult.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses PreparedStatement with parameters and succeeds when credentials are correct")
  void login_usesPreparedStatementWithParameters_andReturnsSuccess() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    String username = "Larry";
    String password = "secretPwd";

    String expectedSql =
        "select password from challenge_users where userid = ? and password = ?";

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(expectedSql)).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Verify that user input is passed as parameters, not concatenated into the SQL string
    org.mockito.Mockito.verify(connection).prepareStatement(expectedSql);
    org.mockito.Mockito.verify(preparedStatement).setString(1, username);
    org.mockito.Mockito.verify(preparedStatement).setString(2, password);
    org.mockito.Mockito.verify(preparedStatement).executeQuery();

    assertEquals("FLAG-5", result.getFeedbackArgs()[0]);
    assertEquals(true, result.getLessonCompleted());
  }

  @Test
  @DisplayName("login with invalid password still uses parameterized query and returns failure")
  void login_invalidPassword_stillUsesParameterizedQuery_andReturnsFailure() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    String username = "Larry";
    String password = "wrongPwd";

    String expectedSql =
        "select password from challenge_users where userid = ? and password = ?";

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(expectedSql)).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    // No rows returned – login should fail
    when(resultSet.next()).thenReturn(false);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    org.mockito.Mockito.verify(connection).prepareStatement(expectedSql);
    org.mockito.Mockito.verify(preparedStatement).setString(1, username);
    org.mockito.Mockito.verify(preparedStatement).setString(2, password);
    org.mockito.Mockito.verify(preparedStatement).executeQuery();

    assertEquals(false, result.getLessonCompleted());
  }
}
