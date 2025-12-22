// Assuming standard package based on resolved_file_path from the workflow.
// If actual package differs, adjust accordingly.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * - PreparedStatement must be used with parameter placeholders.
 * - User input must not be concatenated into the SQL.
 */
public class Assignment5Test {

  private LessonDataSource lessonDataSource;
  private Flags flags;
  private Assignment5 assignment5;

  // JDBC mocks
  private DataSource realDataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    flags = mock(Flags.class);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // Wrap real javax.sql.DataSource inside LessonDataSource if needed.
    // Here we mock LessonDataSource directly to return a Connection.
    lessonDataSource = mock(LessonDataSource.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    assignment5 = new Assignment5(lessonDataSource, flags);
  }

  @Test
  void login_usesParameterizedQuery_andSetsUserAndPasswordParameters() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "S3cureP@ss";
    when(resultSet.next()).thenReturn(true);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert: Prepared SQL must contain placeholders, not concatenated user input
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verifyNoInteractionsWithStringConcatenation(username, password); // semantic guard on test intent
    org.mockito.Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    // The secure fix uses two placeholders:
    // "select password from challenge_users where userid = ? and password = ?"
    // Ensure there are no occurrences of raw username/password in the SQL string.
    org.junit.jupiter.api.Assertions.assertTrue(
        sql.contains("userid = ?") && sql.contains("password = ?"),
        "SQL must use placeholders for userid and password");
    org.junit.jupiter.api.Assertions.assertFalse(
        sql.contains(username) || sql.contains(password),
        "SQL must not contain raw user-controlled values");

    // Ensure parameters are bound in correct order
    org.mockito.Mockito.verify(preparedStatement).setString(1, username);
    org.mockito.Mockito.verify(preparedStatement).setString(2, password);

    // Ensure success path is preserved
    org.junit.jupiter.api.Assertions.assertTrue(result.isLessonCompleted());
    org.mockito.Mockito.verify(flags).getFlag(5);
  }

  @Test
  void login_withEmptyUsernameOrPassword_doesNotTouchDatabase() throws Exception {
    // Arrange: empty username and/or password trigger early validation failure
    String emptyUsername = "   ";
    String nonEmptyPassword = "pwd";

    // Act
    AttackResult result1 = assignment5.login(emptyUsername, nonEmptyPassword);
    AttackResult result2 = assignment5.login("Larry", "  ");

    // Assert: should return failure due to required fields, and must not call DB
    org.junit.jupiter.api.Assertions.assertFalse(result1.isLessonCompleted());
    org.junit.jupiter.api.Assertions.assertFalse(result2.isLessonCompleted());

    // No DB interaction should happen when inputs are invalid
    verifyNoInteractions(connection);
    verifyNoInteractions(preparedStatement);
    verifyNoInteractions(resultSet);
  }

  /**
   * This helper expresses the intent that we are specifically testing for absence of string
   * concatenation-based SQL construction in the updated code. It intentionally does nothing at
   * runtime; assertions on SQL string are in the main test.
   */
  private void verifyNoInteractionsWithStringConcatenation(String username, String password) {
    // Intentionally empty – semantic marker for delta test intent only.
  }
}
