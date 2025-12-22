package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

  @Test
  @DisplayName("login() uses parameterized PreparedStatement instead of string concatenation")
  void login_usesParameterizedPreparedStatement() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(
            connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "correct-password";

    AttackResult result = assignment5.login(username, password);

    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted()).isTrue();

    verify(connection, never())
        .prepareStatement(
            contains("userid = '" + username + "' and password = '" + password + "'"));

    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");

    InOrder inOrder = inOrder(preparedStatement);
    inOrder.verify(preparedStatement).setString(1, username);
    inOrder.verify(preparedStatement).setString(2, password);
    inOrder.verify(preparedStatement).executeQuery();

    verify(connection, times(1)).prepareStatement(anyString());
  }

  @Test
  @DisplayName("login() does not change behavior for invalid credentials while using parameter binding")
  void login_invalidCredentialsStillFail_withParameterizedQuery() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(
            connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "wrong-password";

    AttackResult result = assignment5.login(username, password);

    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted()).isFalse();

    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();
  }
}
