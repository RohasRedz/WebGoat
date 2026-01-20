package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix:
 * verifies that user input is bound as parameters on PreparedStatement
 * and that the correct behavior occurs for matching and non-matching credentials.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses PreparedStatement parameters and succeeds on valid credentials")
  void login_usesPreparedStatementAndSucceedsOnValidCredentials() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "safePassword");

    // Assert
    Mockito.verify(preparedStatement).setString(eq(1), eq("Larry"));
    Mockito.verify(preparedStatement).setString(eq(2), eq("safePassword"));
    assertEquals("success", result.getLessonStatus().toString().toLowerCase());
  }

  @Test
  @DisplayName("login fails when SQL injection payload is supplied instead of bypassing authentication")
  void login_failsOnSqlInjectionPayload() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    // Simulate that DB does not return a row for the injection payload
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String maliciousPassword = "' OR '1'='1";

    // Act
    AttackResult result = assignment5.login("Larry", maliciousPassword);

    // Assert
    Mockito.verify(preparedStatement).setString(eq(1), eq("Larry"));
    Mockito.verify(preparedStatement).setString(eq(2), eq(maliciousPassword));
    assertEquals("failed", result.getLessonStatus().toString().toLowerCase());
  }
}
