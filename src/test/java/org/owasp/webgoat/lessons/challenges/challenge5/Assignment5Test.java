package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta unit tests for {@link Assignment5} focusing on the SQL injection fix.
 *
 * These tests verify:
 * - PreparedStatement is used with parameter binding instead of string concatenation.
 * - Normal login success and failure behavior is preserved.
 * - A typical SQL injection payload does not bypass authentication.
 */
class Assignment5Test {

  @Test
  @DisplayName("login should succeed for correct Larry credentials using bound parameters")
  void loginSucceedsForValidLarryUserWithBoundParameters() throws Exception {
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
    when(flags.getFlag(5)).thenReturn("flag-5-value");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "correct-password";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Verify prepared statement is created with placeholders, NOT with concatenated SQL.
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");

    // Verify bound parameters are passed via setString in the correct order.
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // Ensure the query was executed and login was successful.
    verify(preparedStatement).executeQuery();
    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted()).isTrue();
  }

  @Test
  @DisplayName("login should fail for incorrect password without throwing and without injection")
  void loginFailsForInvalidPasswordAndDoesNotInject() throws Exception {
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
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "wrong-password";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");

    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();

    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted()).isFalse();
  }

  @Test
  @DisplayName("login should not allow classic SQL injection payload to bypass authentication")
  void loginDoesNotAllowSqlInjectionPayload() throws Exception {
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
    // Simulate DB returning no rows for injection payload.
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String injectionPassword = "anything' OR '1'='1";

    // Act
    AttackResult result = assignment5.login(username, injectionPassword);

    // Assert
    // Ensure that the injection string is passed as a bound parameter, not concatenated into SQL.
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();
    assertThat(usedSql)
        .isEqualTo("select password from challenge_users where userid = ? and password = ?");

    // Ensure parameter binding is used with the raw payload (no concatenation).
    verify(preparedStatement).setString(eq(1), eq(username));
    verify(preparedStatement).setString(eq(2), eq(injectionPassword));
    verify(preparedStatement).executeQuery();

    // Injection must not succeed; login should fail.
    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted()).isFalse();
  }
}
