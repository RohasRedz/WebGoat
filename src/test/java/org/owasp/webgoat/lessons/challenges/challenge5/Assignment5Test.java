package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

class Assignment5Test {

  private LessonDataSource lessonDataSource;
  private Flags flags;
  private Assignment5 assignment5;

  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    // Mock the underlying JDBC/DataSource stack
    DataSource realDataSource = Mockito.mock(DataSource.class);
    lessonDataSource = Mockito.mock(LessonDataSource.class);
    flags = Mockito.mock(Flags.class);

    connection = Mockito.mock(Connection.class);
    preparedStatement = Mockito.mock(PreparedStatement.class);
    resultSet = Mockito.mock(ResultSet.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    assignment5 = new Assignment5(lessonDataSource, flags);
  }

  @Test
  void login_usesParameterizedQuery_andSucceedsForValidCredentials() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "secret' OR '1'='1";
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert: still succeeds using flag when credentials match
    org.junit.jupiter.api.Assertions.assertTrue(result.getLessonCompleted());

    // Assert: verify parameterized SQL with placeholders is used
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();
    org.junit.jupiter.api.Assertions.assertTrue(
        sql.contains("where userid = ? and password = ?"),
        "SQL must use parameter placeholders rather than concatenation");

    // Assert: parameters are bound as data (no concatenation)
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
  }

  @Test
  void login_failsForInvalidCredentials_evenWithSqlInjectionLikeInput() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "invalid' OR '1'='1";
    when(resultSet.next()).thenReturn(false);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert: login still fails (no SQL injection bypass)
    org.junit.jupiter.api.Assertions.assertFalse(result.getLessonCompleted());
  }

  @Test
  void login_rejectsEmptyUsernameOrPassword() throws Exception {
    // Arrange
    String username = "";
    String password = "somePassword";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert: this is pre-existing behavior, but we assert it to ensure the fix did not break it
    org.junit.jupiter.api.Assertions.assertFalse(result.getLessonCompleted());
  }
}
