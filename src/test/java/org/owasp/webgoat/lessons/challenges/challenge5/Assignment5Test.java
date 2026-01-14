package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

  @Test
  @DisplayName("login() should use parameterized SQL with user inputs as bind parameters")
  void loginUsesParameterizedSql() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    DataSource dataSource = Mockito.mock(DataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);
    Flags flags = Mockito.mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("flag-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);
    String username = "Larry";
    String password = "anyPassword";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection)
        .prepareStatement(sqlCaptor.capture());
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        sqlCaptor.getValue(),
        "SQL should use placeholders instead of string concatenation for user inputs");

    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    assertTrue(result.getLessonCompleted(), "Successful query should still complete the lesson");
  }
}
