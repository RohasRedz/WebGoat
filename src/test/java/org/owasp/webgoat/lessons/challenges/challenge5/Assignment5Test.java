package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.owasp.webgoat.container.assignments.AttackResult.Type.SUCCESS;
import static org.owasp.webgoat.container.assignments.AttackResult.Type.FAILURE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on the secured SQL path.
 * These tests verify:
 * - The login flow succeeds with valid credentials.
 * - The login flow fails with invalid credentials.
 * - PreparedStatement is used with parameter binding (implicit via mocking).
 */
class Assignment5Test {

  @Test
  void login_withValidCredentials_returnsSuccess() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

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

    AttackResult result = assignment5.login("Larry", "secret");

    assertEquals(SUCCESS, result.getType(), "Login should succeed for valid credentials");
    verify(preparedStatement).setString(1, "Larry");
    verify(preparedStatement).setString(2, "secret");
  }

  @Test
  void login_withInvalidCredentials_returnsFailure() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    AttackResult result = assignment5.login("Larry", "wrong");

    assertEquals(FAILURE, result.getType(), "Login should fail for invalid credentials");
    verify(preparedStatement).setString(1, "Larry");
    verify(preparedStatement).setString(2, "wrong");
  }
}
