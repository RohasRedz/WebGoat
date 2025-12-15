// Package inferred from the source file path; adjust if your test package structure differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta unit tests for {@link Assignment5} focusing only on the SQL injection fix.
 *
 * Verified behavior:
 * 1. Login succeeds only when username == "Larry" and the correct password is supplied.
 * 2. SQL injection attempts via username or password do NOT bypass authentication because the
 *    query uses parameterized PreparedStatement with bound parameters.
 *
 * These tests do not attempt to cover unrelated behavior.
 */
class Assignment5DeltaTest {

  @Test
  @DisplayName("login should succeed only for Larry with correct password")
  void loginSucceedsOnlyForLarryWithCorrectPassword() throws Exception {
    // Arrange
    String correctUsername = "Larry";
    String correctPassword = "secret";

    // Mock DB infrastructure
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    // NOTE: Under the hood LessonDataSource typically wraps a DataSource; we mock only
    // getConnection() behavior here which is what Assignment5 uses.
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true); // Simulate a matching row
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult result = assignment5.login(correctUsername, correctPassword);

    // Assert – functional behavior
    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted()).as("Larry with correct password should succeed").isTrue();

    // Assert – verify parameterized query is used with bound parameters
    InOrder inOrder = Mockito.inOrder(connection, preparedStatement);
    inOrder.verify(connection)
        .prepareStatement("select password from challenge_users where userid = ? and password = ?");
    inOrder.verify(preparedStatement).setString(1, correctUsername);
    inOrder.verify(preparedStatement).setString(2, correctPassword);
    inOrder.verify(preparedStatement).executeQuery();
  }

  @Test
  @DisplayName("login should fail for SQL injection attempt in password due to parameter binding")
  void loginFailsForSqlInjectionAttemptInPassword() throws Exception {
    // Arrange
    String username = "Larry";
    String injectionPassword = "' OR '1'='1";

    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    // No rows should be returned for an injected password when parameters are bound properly
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult result = assignment5.login(username, injectionPassword);

    // Assert – functional behavior: login must fail
    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted())
        .as("SQL injection in password must not bypass authentication")
        .isFalse();

    // Assert – verify parameterized query & binding, not concatenation
    verify(connection)
        .prepareStatement("select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, injectionPassword);
    verify(preparedStatement).executeQuery();
  }

  @Test
  @DisplayName("login should reject SQL injection in username before hitting DB (non-Larry)")
  void loginRejectsSqlInjectionWithNonLarryUsernameBeforeDb() throws Exception {
    // Arrange
    String injectedUsername = "Larry' OR '1'='1";
    String somePassword = "anything";

    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult result = assignment5.login(injectedUsername, somePassword);

    // Assert – functional behavior: immediately fails for non-Larry, no DB-based bypass
    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted())
        .as("Non-Larry username (even with injection) must not succeed")
        .isFalse();

    // Assert – ensure we never reach the DB query: business rule short-circuits before SQL
    verify(lessonDataSource, never()).getConnection();
    verify(connection, never()).prepareStatement(anyString());
    verify(preparedStatement, never()).executeQuery();
  }

  @Test
  @DisplayName("login should fail when username or password is blank (unchanged guard behavior)")
  void loginFailsWhenUsernameOrPasswordBlank() throws Exception {
    // This test helps ensure guards around StringUtils.hasText remain intact
    // and that the SQL-path is not executed for invalid input.

    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    AttackResult resultBlankUser = assignment5.login("   ", "password");
    AttackResult resultBlankPassword = assignment5.login("Larry", "  ");

    assertThat(resultBlankUser.getLessonCompleted()).isFalse();
    assertThat(resultBlankPassword.getLessonCompleted()).isFalse();

    // For blank inputs, we should not even hit the DB
    verify(lessonDataSource, never()).getConnection();
  }
}
