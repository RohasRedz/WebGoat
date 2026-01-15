package org.owasp.webgoat.lessons.challenges.challenge5;

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
import org.springframework.util.StringUtils;

class Assignment5Test {

  @Test
  @DisplayName("login should use parameterized PreparedStatement instead of string concatenation")
  void login_usesParameterizedPreparedStatement() throws Exception {
    // Arrange
    DataSource realDataSource = org.mockito.Mockito.mock(DataSource.class);
    LessonDataSource lessonDataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    Flags flags = org.mockito.Mockito.mock(Flags.class);
    Connection connection = org.mockito.Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("flag-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String username = "Larry";
    String password = "secret";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection)
        .prepareStatement(
            sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();
    // Ensure the query string itself does not contain concatenated user input
    assert usedSql.equals(
        "select password from challenge_users where userid = ? and password = ?");

    // Ensure parameters are bound via setString in correct order
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // And that the query was executed
    verify(preparedStatement).executeQuery();
    assert result.success();
  }
}
