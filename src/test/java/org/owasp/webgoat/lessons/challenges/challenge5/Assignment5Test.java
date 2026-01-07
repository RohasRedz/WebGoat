package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

public class Assignment5Test {

  private LessonDataSource dataSource;
  private Flags flags;
  private Assignment5 assignment5;

  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = mock(LessonDataSource.class);
    flags = mock(Flags.class);
    assignment5 = new Assignment5(dataSource, flags);

    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    when(flags.getFlag(5)).thenReturn("FLAG-5");
  }

  @Test
  void login_withValidLarryCredentials_returnsSuccessAndUsesParameterizedQuery() throws Exception {
    String username = "Larry";
    String password = "secret";
    when(resultSet.next()).thenReturn(true);

    AttackResult result = assignment5.login(username, password);

    assertEquals("success", result.getLessonStatus());
    assertEquals("challenge.solved", result.getMessageId());

    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();
  }

  @Test
  void login_withWrongPassword_returnsFailure() throws Exception {
    String username = "Larry";
    String password = "wrong";
    when(resultSet.next()).thenReturn(false);

    AttackResult result = assignment5.login(username, password);

    assertEquals("failed", result.getLessonStatus());
    assertEquals("challenge.close", result.getMessageId());
  }

  @Test
  void login_withNonLarryUser_returnsFailureWithoutQueryExecution() throws Exception {
    String username = "Bob";
    String password = "whatever";

    AttackResult result = assignment5.login(username, password);

    assertEquals("failed", result.getLessonStatus());
    assertEquals("user.not.larry", result.getMessageId());

    verifyNoInteractions(connection, preparedStatement, resultSet);
  }

  @Test
  void login_withEmptyParameters_triggersRequiredValidation() throws Exception {
    String username = "";
    String password = "   ";

    AttackResult result = assignment5.login(username, password);

    assertEquals("failed", result.getLessonStatus());
    assertEquals("required4", result.getMessageId());

    assertEquals(false, StringUtils.hasText(username));
  }
}
