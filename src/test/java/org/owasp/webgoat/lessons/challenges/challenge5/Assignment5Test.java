package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests focusing on the secure, parameterized query behavior added to Assignment5.login.
 */
class Assignment5Test {

  @Test
  @DisplayName("login() should use PreparedStatement parameters instead of string concatenation")
  void loginUsesParameterizedQuery() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    String username = "Larry";
    String password = "SecurePassword' OR '1'='1";

    // Act
    assignment5.login(username, password);

    // Assert
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, eq(username));
    verify(preparedStatement).setString(2, eq(password));
  }

  @Test
  @DisplayName("login() should not leak SQLExceptions and still handle failures gracefully")
  void loginHandlesSqlExceptionWithoutPropagatingDetails() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = Mockito.mock(Connection.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenThrow(new SQLException("synthetic SQL error"));

    String username = "Larry";
    String password = "any";

    // Act & Assert
    // We expect no SQLException to be propagated; the method should handle it internally.
    assignment5.login(username, password);
  }
}
