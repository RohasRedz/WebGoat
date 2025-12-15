package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
 * - SQL query must be parameterized, not constructed via string concatenation.
 * - The flow still uses the provided username and password values.
 */
class Assignment5Test {

  @Test
  @DisplayName("login() should use PreparedStatement parameters instead of concatenated SQL")
  void loginUsesParameterizedSql() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "somePassword";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    verify(connection, times(1))
        .prepareStatement("select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement, times(1)).setString(1, username);
    verify(preparedStatement, times(1)).setString(2, password);
    verify(preparedStatement, times(1)).executeQuery();

    assertTrue(result.getLessonCompleted(), "Expected challenge to be marked as solved");
  }

  @Test
  @DisplayName("login() should fail for non-Larry users even with valid credentials")
  void loginFailsForNonLarryUser() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Bob";
    String password = "whatever";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    assertFalse(
        result.getLessonCompleted(),
        "Non-Larry users must not succeed even if the SQL is parameterized securely");
  }

  @Test
  @DisplayName("login() should still enforce required username and password")
  void loginRequiresUsernameAndPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult resultEmptyUser = assignment5.login("", "password");
    AttackResult resultEmptyPass = assignment5.login("Larry", "");

    // Assert
    assertFalse(resultEmptyUser.getLessonCompleted(), "Empty username must not be accepted");
    assertFalse(resultEmptyPass.getLessonCompleted(), "Empty password must not be accepted");
  }
}
