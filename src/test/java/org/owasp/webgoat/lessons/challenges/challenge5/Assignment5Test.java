package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on the fixed SQL query:
 * - Verifies that user input is bound as parameters rather than concatenated into SQL.
 * - Detects typical SQL injection payload behavior (payload must not change logic).
 *
 * NOTE: We cannot assert the literal SQL string content used in PreparedStatement here,
 * but we can assert how parameters are bound and that a malicious password does not
 * alter the logic.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses PreparedStatement parameters so SQL injection payload does not bypass authentication")
  void loginDoesNotAllowSqlInjectionViaPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);
    Flags flags = Mockito.mock(Flags.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    // Simulate that the query does not return any row for injected password
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String maliciousPassword = "' OR '1'='1";

    // Act
    AttackResult result = assignment5.login(username, maliciousPassword);

    // Assert
    // Ensure parameters are bound exactly as provided, meaning the injection string
    // is treated as data and not as part of the SQL logic.
    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, maliciousPassword);

    // Because the result set has no rows, authentication must fail even with SQL injection payload.
    assertTrue(result.getLessonCompleted() == false, "Authentication should fail for injection payload");
    assertEquals("challenge.close", result.getFeedback(), "Expected failure feedback for incorrect password");
  }

  @Test
  @DisplayName("login succeeds when correct username and password are provided and parameters are bound")
  void loginSucceedsWithCorrectCredentialsUsingParameters() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);
    Flags flags = Mockito.mock(Flags.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String correctPassword = "superSecret";

    // Act
    AttackResult result = assignment5.login(username, correctPassword);

    // Assert
    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, correctPassword);

    assertTrue(result.getLessonCompleted(), "Lesson should be marked as completed for correct credentials");
    assertEquals("challenge.solved", result.getFeedback(), "Expected success feedback");
  }
}
