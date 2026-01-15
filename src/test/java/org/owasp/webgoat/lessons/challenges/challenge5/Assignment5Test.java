package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

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
 * Delta tests for Assignment5 focusing only on the SQL injection fix.
 *
 * These tests verify that:
 * - The login logic uses a parameterized PreparedStatement with placeholders.
 * - User input is no longer concatenated into the SQL string.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized PreparedStatement and does not concatenate user input")
  void loginUsesParameterizedQuery() throws Exception {
    // Arrange
    String username = "Larry' OR '1'='1";
    String password = "somePass";

    // Mock JDBC interaction
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(
            connection.prepareStatement(
                Mockito.anyString()))
        .thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(true);

    // Mock Flags so that success path can be executed without NPE
    Flags flags = Mockito.mock(Flags.class);
    Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // 1) Verify query string does not contain the raw username or password
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();

    // The SQL must use placeholders instead of concatenated user input
    assertTrue(
        usedSql.contains("where userid = ? and password = ?"),
        "SQL should use parameter placeholders instead of inlined user input");
    // Ensure the potentially malicious username is not directly present in the SQL string
    assertTrue(
        !usedSql.contains(username),
        "SQL must not contain the raw username value");
    assertTrue(
        !usedSql.contains(password),
        "SQL must not contain the raw password value");

    // 2) Verify parameters are bound separately in correct order
    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, password);

    // 3) Verify behavior remains success when ResultSet has a row
    assertTrue(result.isSuccess(), "Login should still succeed functionally when a row is returned");
  }
}
