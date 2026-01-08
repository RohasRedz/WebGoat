// File path: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

  private LessonDataSource dataSource;
  private Flags flags;
  private Assignment5 assignment5;

  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @BeforeEach
  void setup() throws Exception {
    dataSource = mock(LessonDataSource.class);
    flags = mock(Flags.class);
    assignment5 = new Assignment5(dataSource, flags);

    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(flags.getFlag(5)).thenReturn("FLAG-5");
  }

  @Test
  void login_shouldUseParameterizedQueryAndAuthenticateValidLarry() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "secret";
    when(resultSet.next()).thenReturn(true);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    verify(connection)
        .prepareStatement(
            eq("select password from challenge_users where userid = ? and password = ?"));
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();

    // Ensure a successful result is returned when DB reports a matching row
    assertEquals(true, result.getLessonCompleted());
  }

  @Test
  void login_shouldFailForSqlInjectionLikePasswordEvenIfRowNotReturned() throws Exception {
    // Arrange
    String username = "Larry";
    String injectionPassword = "' OR '1'='1";
    // Simulate no matching row returned by DB despite injection attempt
    when(resultSet.next()).thenReturn(false);

    // Act
    AttackResult result = assignment5.login(username, injectionPassword);

    // Assert: still using parameterized query, not concatenation
    verify(connection)
        .prepareStatement(
            eq("select password from challenge_users where userid = ? and password = ?"));
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, injectionPassword);
    verify(preparedStatement).executeQuery();

    // No lesson completion since DB rejects the injected password
    assertEquals(false, result.getLessonCompleted());
  }
}
