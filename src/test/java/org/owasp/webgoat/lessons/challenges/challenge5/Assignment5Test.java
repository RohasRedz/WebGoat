package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

public class Assignment5Test {

  @Test
  @DisplayName("login should use parameterized PreparedStatement instead of concatenated SQL")
  void login_usesParameterizedPreparedStatement() throws Exception {
    // Arrange
    DataSource realDataSource = Mockito.mock(DataSource.class);
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);
    Flags flags = Mockito.mock(Flags.class);

    Mockito.when(lessonDataSource.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(true);
    Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 endpoint = new Assignment5(lessonDataSource, flags);

    String username = "Larry";
    String password = "secret";

    // Act
    AttackResult result = endpoint.login(username, password);

    // Assert
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(connection).prepareStatement(queryCaptor.capture());
    String usedQuery = queryCaptor.getValue();

    // The query must contain placeholders instead of concatenated user input
    assertTrue(
        usedQuery.contains("userid = ?"),
        "Expected query to use parameter placeholder for userid");
    assertTrue(
        usedQuery.contains("password = ?"),
        "Expected query to use parameter placeholder for password");
    // Ensure no direct concatenation of username or password into the SQL string
    assertTrue(
        !usedQuery.contains(username) && !usedQuery.contains(password),
        "Query must not contain raw user input values");

    // Verify parameters are bound correctly
    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, password);

    // And the call still succeeds functionally
    assertTrue(result.getLessonCompleted(), "Login should still succeed when resultSet has a row");
  }

  @Test
  @DisplayName("login should reject blank username or password (unchanged guard but ensures safe behavior)")
  void login_rejectsBlankInputs() throws Exception {
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 endpoint = new Assignment5(dataSource, flags);

    AttackResult result1 = endpoint.login("", "pwd");
    AttackResult result2 = endpoint.login("Larry", " ");
    AttackResult result3 = endpoint.login(null, "pwd");
    AttackResult result4 = endpoint.login("Larry", null);

    assertTrue(
        !result1.getLessonCompleted(), "Blank username must not complete the lesson successfully");
    assertTrue(
        !result2.getLessonCompleted(), "Blank password must not complete the lesson successfully");
    assertTrue(
        !result3.getLessonCompleted(), "Null username must not complete the lesson successfully");
    assertTrue(
        !result4.getLessonCompleted(), "Null password must not complete the lesson successfully");
  }
}
