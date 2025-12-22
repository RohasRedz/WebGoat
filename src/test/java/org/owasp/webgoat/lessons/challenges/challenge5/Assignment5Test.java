package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the modified SQL query behavior:
 * - Successful login for the valid user 'Larry' with correct password.
 * - SQL injection-style payloads must not bypass authentication.
 */
class Assignment5Test {

  @Test
  @DisplayName("login should succeed for user Larry with correct password using parameterized query")
  void loginSucceedsForLarryWithCorrectPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "correct-password");

    // Assert
    assertNotNull(result, "AttackResult should not be null");
    // Success feedback key should be used on successful authentication
    assertEquals("challenge.solved", result.getLessonResult().getFeedback(), "Expected success feedback");
    // Verify that the PreparedStatement uses parameters instead of string concatenation
    verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, "Larry");
    verify(preparedStatement).setString(2, "correct-password");
    verify(preparedStatement).executeQuery();
  }

  @Test
  @DisplayName("login should not be bypassable via SQL injection payloads in username or password")
  void loginShouldNotAllowSqlInjectionBypass() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    // Simulate no matching row for malicious payload
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String maliciousPassword = "' OR '1'='1";

    // Act
    AttackResult result = assignment5.login("Larry", maliciousPassword);

    // Assert
    assertNotNull(result, "AttackResult should not be null");
    // Feedback key for a close-but-incorrect attempt should be used
    assertEquals("challenge.close", result.getLessonResult().getFeedback(), "Expected failure feedback");
    // Ensure parameters are passed as values, not concatenated into SQL
    verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, "Larry");
    verify(preparedStatement).setString(2, maliciousPassword);
    verify(preparedStatement).executeQuery();
  }

  @Test
  @DisplayName("login should reject non-Larry usernames even with SQL injection patterns")
  void loginRejectsNonLarryUsernamesWithSqlInjectionPatterns() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result =
        assignment5.login("Larry' OR '1'='1", "does-not-matter");

    // Assert
    assertNotNull(result);
    assertEquals(
        "user.not.larry",
        result.getLessonResult().getFeedback(),
        "Usernames that are not exactly 'Larry' should be rejected before hitting the database");
    // Ensure no DB interaction occurs when username is not exactly Larry
    verifyNoInteractions(dataSource);
  }
}
