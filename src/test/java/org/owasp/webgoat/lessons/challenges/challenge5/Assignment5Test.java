package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on the parameterized SQL behavior:
 * - Verifies that the login method uses PreparedStatement with parameter placeholders.
 * - Verifies that user inputs are bound via setString rather than concatenated into the SQL.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login should use parameterized query and bind username and password via setString")
  void login_usesParameterizedQueryAndBindsParameters() throws Exception {
    // Arrange
    LessonDataSource dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    Flags flags = org.mockito.Mockito.mock(Flags.class);

    Connection connection = org.mockito.Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(org.mockito.Mockito.anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("dummy-flag");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "password123";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());

    String usedSql = sqlCaptor.getValue();
    // Ensure SQL contains placeholders instead of concatenated user input
    org.junit.jupiter.api.Assertions.assertTrue(
        usedSql.contains("userid = ?"),
        "SQL should use parameter placeholder for userid");
    org.junit.jupiter.api.Assertions.assertTrue(
        usedSql.contains("password = ?"),
        "SQL should use parameter placeholder for password");
    org.junit.jupiter.api.Assertions.assertFalse(
        usedSql.contains(username),
        "SQL must not contain raw username");
    org.junit.jupiter.api.Assertions.assertFalse(
        usedSql.contains(password),
        "SQL must not contain raw password");

    // Ensure parameters are bound via setString in correct order
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();
    verifyNoMoreInteractions(preparedStatement);

    org.junit.jupiter.api.Assertions.assertTrue(
        result.getLessonCompleted(),
        "Successful query result should still complete the lesson");
  }

  @Test
  @DisplayName("login should not execute query when username or password is blank")
  void login_returnsFailureWithoutExecutingQueryForBlankInput() throws Exception {
    // Arrange
    LessonDataSource dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    Flags flags = org.mockito.Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult resultNoUser = assignment5.login("", "secret");
    AttackResult resultNoPassword = assignment5.login("Larry", "");

    // Assert
    org.junit.jupiter.api.Assertions.assertFalse(
        resultNoUser.getLessonCompleted(),
        "Blank username should not complete lesson");
    org.junit.jupiter.api.Assertions.assertFalse(
        resultNoPassword.getLessonCompleted(),
        "Blank password should not complete lesson");

    // Since validation short-circuits, dataSource should never be called
    verifyNoMoreInteractions(dataSource);
  }
}
