/*
 * Unit tests for Assignment5 to validate parameterized SQL usage and security behavior.
 */

package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

  private LessonDataSource createLessonDataSourceMock(Connection connection) throws Exception {
    DataSource ds = Mockito.mock(DataSource.class);
    when(ds.getConnection()).thenReturn(connection);
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    when(lessonDataSource.getConnection()).thenReturn(connection);
    return lessonDataSource;
  }

  @Test
  @DisplayName("login uses parameterized PreparedStatement and does not concatenate user input into SQL")
  void login_usesParameterizedQuery_noSqlInjectionConcatenation() throws Exception {
    String username = "Larry";
    String password = "anyPassword";

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    LessonDataSource lessonDataSource = createLessonDataSourceMock(connection);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    AttackResult result = assignment5.login(username, password);

    Mockito.verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, password);
    Mockito.verify(preparedStatement).executeQuery();

    assertEquals("challenge.close", result.getOutput());
  }

  @Test
  @DisplayName("login rejects obvious SQL injection payload via parameterized query (no shortcut success)")
  void login_rejectsSqlInjectionPayload() throws Exception {
    String username = "Larry";
    String injectionPassword = "anything' OR '1'='1";

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    LessonDataSource lessonDataSource = createLessonDataSourceMock(connection);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    AttackResult result = assignment5.login(username, injectionPassword);

    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, injectionPassword);
    assertEquals("challenge.close", result.getOutput());
  }

  @Test
  @DisplayName("login still enforces non-empty and specific user 'Larry' checks after fix")
  void login_preservesInputValidationAndUserCheck() throws Exception {
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    LessonDataSource lessonDataSource = createLessonDataSourceMock(connection);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    AttackResult emptyUserResult = assignment5.login("", "pw");
    assertEquals("required4", emptyUserResult.getOutput());

    AttackResult notLarryResult = assignment5.login("Bob", "pw");
    assertEquals("user.not.larry", notLarryResult.getOutput());
  }
}
