package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query and succeeds for valid credentials")
  void loginUsesPreparedStatementAndSucceedsForValidUser() throws Exception {
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
    when(flags.getFlag(5)).thenReturn("flag-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "secret");

    // Assert
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, "Larry");
    verify(preparedStatement).setString(2, "secret");
    verify(preparedStatement).executeQuery();
    // Success path should be taken when a row is present
    assert result.getLessonCompleted();
  }

  @Test
  @DisplayName("login fails for invalid credentials while still using parameterized SQL")
  void loginFailsForInvalidCredentials() throws Exception {
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
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "wrong-password");

    // Assert
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, "Larry");
    verify(preparedStatement).setString(2, "wrong-password");
    verify(preparedStatement).executeQuery();
    // No row  failure result
    assert !result.getLessonCompleted();
  }

  @Test
  @DisplayName("login rejects non-Larry usernames before hitting the database")
  void loginRejectsNonLarryUserBeforeDb() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Mallory", "whatever");

    // Assert
    // Should not go to DB at all for non-Larry usernames; ensures parameterized
    // query path is only used for expected user.
    assert !result.getLessonCompleted();
  }
}
