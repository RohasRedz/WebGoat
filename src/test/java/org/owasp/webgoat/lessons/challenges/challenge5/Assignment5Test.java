package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on the fixed SQL injection vulnerability.
 *
 * These tests verify that:
 * - The query uses parameterized PreparedStatement instead of string concatenation.
 * - The method still returns success on valid credentials and failure on invalid credentials.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query and succeeds for valid credentials")
  void login_usesParameterizedQuery_andSucceedsForValidCredentials() throws Exception {
    // Arrange
    DataSource dataSource = mock(DataSource.class);
    LessonDataSource lessonDataSource = new LessonDataSource(dataSource);
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
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "secret");

    // Assert: success path preserved
    assertSame(AttackResult.Status.SUCCESS, result.getLessonStatus());

    // Assert: verify that parameterized query is used with bind variables
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection)
        .prepareStatement(sqlCaptor.capture());
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        sqlCaptor.getValue());

    verify(preparedStatement).setString(1, "Larry");
    verify(preparedStatement).setString(2, "secret");
  }

  @Test
  @DisplayName("login fails when credentials do not match, using parameterized query")
  void login_usesParameterizedQuery_andFailsForInvalidCredentials() throws Exception {
    // Arrange
    DataSource dataSource = mock(DataSource.class);
    LessonDataSource lessonDataSource = new LessonDataSource(dataSource);
    Flags flags = mock(Flags.class);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    // No results -> invalid credentials
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "wrong-password");

    // Assert: failure path preserved
    assertSame(AttackResult.Status.FAIL, result.getLessonStatus());

    // Assert: still using parameterized query with bound parameters
    verify(connection)
        .prepareStatement(eq("select password from challenge_users where userid = ? and password = ?"));
    verify(preparedStatement).setString(1, "Larry");
    verify(preparedStatement).setString(2, "wrong-password");
  }
}
