package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on the fixed SQL injection behavior.
 *
 * Expected secure behavior:
 * - SQL query must be executed via a PreparedStatement with parameter placeholders.
 * - User supplied values (username_login, password_login) must be bound via setString
 *   instead of being concatenated into the SQL string.
 */
public class Assignment5Test {

  private LessonDataSource dataSource;
  private Flags flags;
  private Assignment5 assignment;

  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = mock(LessonDataSource.class);
    flags = mock(Flags.class);
    assignment = new Assignment5(dataSource, flags);

    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }

  @Test
  void login_usesParameterizedQueryWithUserInputs() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "' OR '1'='1"; // classic SQLi payload should be treated as data
    when(resultSet.next()).thenReturn(false);

    // Act
    AttackResult result = assignment.login(username, password);

    // Assert
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();

    // still should fail login because no row is returned
    assertTrue(result.getLessonCompleted() == false);
  }

  @Test
  void login_succeedsForValidUserAndPassword() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "secret";
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // Act
    AttackResult result = assignment.login(username, password);

    // Assert
    assertTrue(result.getLessonCompleted());
    // ensure we actually used the same username/password parameters
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
  }

  @Test
  void login_rejectsNonLarryUsersBeforeQueryExecution() throws Exception {
    // Arrange
    String username = "Mallory";
    String password = "anything";

    // Act
    AttackResult result = assignment.login(username, password);

    // Assert
    // For non-Larry users, the code should bail out before preparing/executing any SQL
    assertTrue(result.getLessonCompleted() == false);
  }
}
