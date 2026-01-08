package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.eq;
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
 * Delta tests for Assignment5 focusing on the fixed parameterized SQL query behavior.
 *
 * Derived test path (per instructions):
 * src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query instead of string concatenation")
  void login_usesParameterizedQueryWithUserInputs() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true); // simulate successful login
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "SecurePass123";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, eq(username));
    verify(preparedStatement).setString(2, eq(password));

    // Behavior: success path still works with valid credentials
    // (focus here is that the fixed path returns a success result when query returns a row)
    assert result.isLessonCompleted();
  }
}
