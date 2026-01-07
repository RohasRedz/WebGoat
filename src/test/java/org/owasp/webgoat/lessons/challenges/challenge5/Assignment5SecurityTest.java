package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Delta/security-focused tests for Assignment5.
 *
 * These tests focus only on the behavior changed by the fix:
 * - Use of parameterized PreparedStatement instead of SQL string concatenation.
 * - Ensuring that attempted SQL injection does not bypass authentication logic.
 *
 * Note: We are not testing every branch, only the behavior related to the
 * vulnerability and its remediation.
 */
public class Assignment5SecurityTest {

  @Test
  @DisplayName("login succeeds with correct credentials for Larry (happy path, regression guard)")
  void loginSucceedsWithValidCredentials() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(true);
    Mockito.when(flags.getFlag(5)).thenReturn("FLAG5");

    // Act
    AttackResult result = assignment5.login("Larry", "correct-password");

    // Assert
    assertTrue(result.getLessonCompleted(), "Expected lesson to be completed for valid credentials");
    // Also assert that SQL was parameterized (2 parameters set) and not concatenated.
    Mockito.verify(connection)
        .prepareStatement("select password from challenge_users where userid = ? and password = ?");
    Mockito.verify(preparedStatement).setString(1, "Larry");
    Mockito.verify(preparedStatement).setString(2, "correct-password");
  }

  @Test
  @DisplayName("login fails for SQL injection attempt in username (no authentication bypass via injection)")
  void loginFailsForSqlInjectionInUsername() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    // Simulate DB indicating no matching row (injection should not make this true)
    Mockito.when(resultSet.next()).thenReturn(false);

    // Injection-style username (previously dangerous when SQL was concatenated)
    String injectionUsername = "Larry' OR '1'='1";
    String password = "anything";

    // Act
    AttackResult result = assignment5.login(injectionUsername, password);

    // Assert
    assertTrue(
        !result.getLessonCompleted(),
        "SQL injection-style username must not bypass authentication or complete lesson");

    // Verify input validation still applies and that the query is parameterized.
    Mockito.verify(connection)
        .prepareStatement("select password from challenge_users where userid = ? and password = ?");
    Mockito.verify(preparedStatement).setString(1, injectionUsername);
    Mockito.verify(preparedStatement).setString(2, password);
  }

  @Test
  @DisplayName("login fails when username is not exactly 'Larry' even if password is correct")
  void loginFailsForNonLarryUser() throws Exception {
    // This test ensures the guard clause before the SQL execution still behaves as expected.
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // No DB interactions should occur when username != Larry
    AttackResult result = assignment5.login("NotLarry", "some-password");

    assertTrue(
        !result.getLessonCompleted(),
        "Non-Larry user should not pass even with any password");
    Mockito.verifyNoInteractions(dataSource);
  }
}
