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

public class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query and returns success for valid credentials")
  void login_usesPreparedStatementAndReturnsSuccess() throws Exception {
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
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "secure-password");

    // Assert
    assertEquals("success", result.getLessonStatus().toString().toLowerCase());
  }

  @Test
  @DisplayName("login fails when username is not Larry even if password is provided")
  void login_rejectsNonLarryUser() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Mallory", "anything");

    // Assert
    assertEquals("failed", result.getLessonStatus().toString().toLowerCase());
  }

  @Test
  @DisplayName("login fails when username or password is empty (input presence validation)")
  void login_requiresNonEmptyUsernameAndPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult resultEmptyUser = assignment5.login("", "pwd");
    AttackResult resultEmptyPwd = assignment5.login("Larry", "");

    // Assert
    assertEquals("failed", resultEmptyUser.getLessonStatus().toString().toLowerCase());
    assertEquals("failed", resultEmptyPwd.getLessonStatus().toString().toLowerCase());
  }
}
