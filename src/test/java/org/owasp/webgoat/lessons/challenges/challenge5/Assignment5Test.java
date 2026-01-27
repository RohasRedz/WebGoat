package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on the SQL parameterization change:
 * - ensure PreparedStatement uses `?` placeholders and that user input is bound via setString
 * - ensure overall behavior (success/failure) is preserved for the happy path.
 *
 * This test assumes that the updated Assignment5 uses:
 *   connection.prepareStatement("select password from challenge_users where userid = ? and password = ?");
 *   statement.setString(1, username_login);
 *   statement.setString(2, password_login);
 */
public class Assignment5Test {

  @Mock
  private LessonDataSource dataSource;

  @Mock
  private Flags flags;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement preparedStatement;

  @Mock
  private ResultSet resultSet;

  private Assignment5 assignment5;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    assignment5 = new Assignment5(dataSource, flags);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }

  @Test
  void login_usesParameterizedQueryAndBindsUserInput() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "p@ssw0rd";
    when(resultSet.next()).thenReturn(true);
    AttackResult expectedResult =
        AttackResult.success(assignment5).feedback("challenge.solved").build();
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert: statement SQL uses placeholders and parameters are bound in order
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection, times(1)).prepareStatement(sqlCaptor.capture());
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        sqlCaptor.getValue(),
        "SQL should use parameter placeholders instead of string concatenation");

    verify(preparedStatement, times(1)).setString(1, username);
    verify(preparedStatement, times(1)).setString(2, password);

    // Assert: query is executed and success path is taken
    verify(preparedStatement, times(1)).executeQuery();
    verify(resultSet, times(1)).next();
    verify(flags, times(1)).getFlag(5);
  }

  @Test
  void login_failsWhenCredentialsDoNotMatchButStillUseParameters() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "wrong-password";
    when(resultSet.next()).thenReturn(false);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert: parameters are still bound via PreparedStatement
    verify(preparedStatement, times(1)).setString(1, username);
    verify(preparedStatement, times(1)).setString(2, password);
    verify(preparedStatement, times(1)).executeQuery();
    verify(resultSet, times(1)).next();

    // We do not assert exact AttackResult contents, only that the flow reaches the DB safely.
  }
}
