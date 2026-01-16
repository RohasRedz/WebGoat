package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing only on the updated SQL query behavior:
 * - Ensures the PreparedStatement uses parameter placeholders instead of string concatenation.
 * - Ensures user-supplied username and password are passed via setString parameters.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query and binds username and password via setString")
  void login_usesParameterizedQuery_andBindsUserInputs() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "p@ssw0rd";

    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);
    Flags flags = Mockito.mock(Flags.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();
    // The fixed code must use parameter placeholders instead of concatenating user input
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        usedSql,
        "SQL should use parameter placeholders and not concatenate user input");

    // Verify that user-controlled values are bound via setString on the PreparedStatement
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // Also verify behavior is still successful when query returns a row
    // (guards against regressions in control flow)
    org.junit.jupiter.api.Assertions.assertTrue(result.isSuccess());
  }

  @Test
  @DisplayName("login still rejects non-Larry users – behavior unchanged by fix")
  void login_rejectsNonLarryUsers_unaffectedBySqlFix() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("NotLarry", "any");

    // Assert
    // This is a guard to ensure the security fix did not change the existing access control rule
    org.junit.jupiter.api.Assertions.assertFalse(result.isSuccess());
  }

  @Test
  @DisplayName("login returns failure for missing username or password – input validation unchanged")
  void login_failsOnMissingInput_unaffectedBySqlFix() throws Exception {
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    AttackResult resultEmptyUser = assignment5.login("", "pass");
    AttackResult resultEmptyPass = assignment5.login("Larry", "");

    org.junit.jupiter.api.Assertions.assertFalse(resultEmptyUser.isSuccess());
    org.junit.jupiter.api.Assertions.assertFalse(resultEmptyPass.isSuccess());
  }
}
