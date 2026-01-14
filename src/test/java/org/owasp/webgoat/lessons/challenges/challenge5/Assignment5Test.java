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
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Delta tests for Assignment5 focusing on the SQL parameterization change.
 *
 * These tests assert that:
 * - The login method uses a parameterized PreparedStatement with placeholders.
 * - User-supplied values are bound via setString rather than concatenated into the SQL.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized PreparedStatement with placeholders and bound parameters")
  void login_usesPreparedStatementParameters() throws Exception {
    // Arrange
    LessonDataSource dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    Flags flags = org.mockito.Mockito.mock(Flags.class);

    Connection connection = org.mockito.Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);

    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("dummy-flag");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "secretPassword");

    // Assert
    String sql = sqlCaptor.getValue();
    // Ensure the query now uses placeholders instead of direct concatenation
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        sql,
        "SQL should use parameter placeholders instead of concatenating user input");

    // Ensure user inputs are bound via setString on the PreparedStatement
    verify(preparedStatement).setString(1, "Larry");
    verify(preparedStatement).setString(2, "secretPassword");

    // Sanity check that the call still succeeds
    org.junit.jupiter.api.Assertions.assertTrue(result.getLessonCompleted());
  }
}
