package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.owasp.webgoat.container.assignments.AttackResult.Status.SUCCESS;
import static org.owasp.webgoat.container.assignments.AttackResult.Status.FAIL;

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
 * Delta tests for Assignment5 focusing on the secured, parameterized SQL query behavior.
 *
 * Test file path (derived by replacing /main/ with /test/):
 * src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 */
public class Assignment5Test {

  @Test
  @DisplayName("login should use parameterized PreparedStatement and succeed for valid Larry credentials")
  void login_usesParameterizedQuery_andAuthenticatesLarry() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(
            connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "safePassword");

    // Assert
    Mockito.verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    Mockito.verify(preparedStatement).setString(1, "Larry");
    Mockito.verify(preparedStatement).setString(2, "safePassword");
    Mockito.verify(preparedStatement).executeQuery();

    assertEquals(SUCCESS, result.getStatus());
  }

  @Test
  @DisplayName("login should fail when password does not match while still using parameterized query")
  void login_failsWithWrongPassword_usingParameterizedQuery() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(
            connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "wrongPassword");

    // Assert
    Mockito.verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    Mockito.verify(preparedStatement).setString(1, "Larry");
    Mockito.verify(preparedStatement).setString(2, "wrongPassword");
    Mockito.verify(preparedStatement).executeQuery();

    assertEquals(FAIL, result.getStatus());
  }
}
