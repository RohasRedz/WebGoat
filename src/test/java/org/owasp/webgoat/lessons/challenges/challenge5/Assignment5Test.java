package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the parameterized query behavior
 * introduced to fix SQL injection by eliminating string concatenation of user input.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login() should use parameterized query with user input bound as parameters")
  void login_usesParameterizedQueryAndBindsUserInput() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito
        .when(connection.prepareStatement(Mockito.anyString()))
        .thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(true);
    Mockito.when(flags.getFlag(5)).thenReturn("flag-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "SecurePassword' OR '1'='1";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // 1) Ensure query now uses placeholders instead of inlined user input
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();
    assertTrue(
        usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
        "SQL should use parameter placeholders instead of concatenating user input");

    // 2) Ensure user input is passed via setString rather than concatenated into SQL
    ArgumentCaptor<String> paramCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(preparedStatement, Mockito.times(2)).setString(Mockito.anyInt(), paramCaptor.capture());

    // Order of parameters should be: username, then password
    assertEquals("Larry", paramCaptor.getAllValues().get(0), "First parameter must be username_login");
    assertEquals(password, paramCaptor.getAllValues().get(1), "Second parameter must be password_login");

    // 3) Success path behavior still works (regression check)
    assertTrue(result.getLessonCompleted(), "Login should still succeed when correct user/password is used");
  }
}
