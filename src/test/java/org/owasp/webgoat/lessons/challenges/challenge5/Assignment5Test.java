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

/**
 * Delta tests for Assignment5 focusing on the fixed SQL injection vulnerability.
 *
 * The updated code changed the query from string concatenation with user input to
 * a parameterized PreparedStatement with placeholders and setString calls.
 *
 * These tests verify:
 * - The SQL query string no longer embeds user input directly.
 * - The user-supplied username and password are passed as bound parameters.
 * - Successful login path still works when the query returns a row.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query and binds username and password correctly")
  void login_usesParameterizedQuery_andBindsUserInput() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    DataSource dataSource = Mockito.mock(DataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);
    Flags flags = Mockito.mock(Flags.class);

    Mockito.when(lessonDataSource.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(true);
    Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String username = "Larry";
    String password = "S3cureP@ss";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(connection).prepareStatement(sqlCaptor.capture());

    String usedSql = sqlCaptor.getValue();
    // Ensure the SQL uses placeholders instead of concatenated user input
    assertTrue(
        usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
        "SQL must use placeholders for userid and password");

    // Ensure the raw user input is not concatenated into the SQL string
    assertTrue(
        !usedSql.contains(username) && !usedSql.contains(password),
        "SQL must not directly contain user-supplied username or password");

    // Verify bound parameters preserve original semantics (username and password are set correctly)
    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, password);

    // Also verify that successful path still behaves as expected
    assertTrue(result.getOutput().contains("challenge.solved"));
    assertTrue(result.getOutput().contains("FLAG-5"));
  }

  @Test
  @DisplayName("login fails when credentials are incorrect while still using parameterized query")
  void login_failsWithIncorrectCredentials_stillParameterized() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    DataSource dataSource = Mockito.mock(DataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);
    Flags flags = Mockito.mock(Flags.class);

    Mockito.when(lessonDataSource.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String username = "Larry";
    String password = "WrongPassword";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(connection).prepareStatement(sqlCaptor.capture());

    String usedSql = sqlCaptor.getValue();
    assertTrue(
        usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
        "SQL must use placeholders for userid and password even on failure");

    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, password);

    assertTrue(result.getOutput().contains("challenge.close"));
  }
}
