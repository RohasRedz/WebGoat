package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
 * Delta tests for Assignment5 focusing on the fixed parameterized SQL behavior.
 *
 * Test file path (derived): src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 */
class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query and succeeds for valid Larry credentials")
  void login_withValidLarryCredentials_usesParameterizedQueryAndSucceeds() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

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

    // Act
    AttackResult result = assignment5.login("Larry", "secret");

    // Assert
    Mockito.verify(preparedStatement).setString(1, "Larry");
    Mockito.verify(preparedStatement).setString(2, "secret");
    assertTrue(result.getLessonCompleted(), "Expected challenge to be marked as solved");
    assertEquals("FLAG-5", result.getFeedbackArgs()[0]);
  }

  @Test
  @DisplayName("login fails when username is not Larry even though query is parameterized")
  void login_withNonLarryUser_stillFailsWithParameterizedQuery() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Mallory", "secret");

    // Assert
    assertTrue(!result.getLessonCompleted(), "Non-Larry user must not solve the challenge");
  }
}
