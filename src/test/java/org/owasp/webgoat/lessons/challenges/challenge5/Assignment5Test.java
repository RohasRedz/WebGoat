package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

  @Test
  @DisplayName("login() should use parameterized query and succeed for valid Larry credentials")
  void loginUsesParameterizedQueryAndSucceedsForValidUser() throws Exception {
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
    when(flags.getFlag(5)).thenReturn("flag-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "secret");

    // Assert
    Mockito.verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    Mockito.verify(preparedStatement).setString(1, "Larry");
    Mockito.verify(preparedStatement).setString(2, "secret");
    assertEquals(AttackResult.Status.SUCCESS, result.getLessonStatus());
  }

  @Test
  @DisplayName("login() should fail when username or password is empty (input validation unchanged)")
  void loginFailsOnEmptyCredentials() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult resultEmptyUser = assignment5.login("", "secret");
    AttackResult resultEmptyPassword = assignment5.login("Larry", "");

    // Assert
    assertEquals(AttackResult.Status.FAIL, resultEmptyUser.getLessonStatus());
    assertEquals(AttackResult.Status.FAIL, resultEmptyPassword.getLessonStatus());
  }
}
