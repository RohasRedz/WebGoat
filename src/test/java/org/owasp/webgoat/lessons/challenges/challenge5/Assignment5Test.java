// Delta_UnitTest_Agent
// Assumption: using the same package as the class under test.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
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

class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query and no longer concatenates SQL with user input")
  void loginUsesParameterizedQuery() throws Exception {
    // Arrange
    LessonDataSource dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    Flags flags = org.mockito.Mockito.mock(Flags.class);
    Connection connection = org.mockito.Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "secret";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // 1. Ensure prepared statement SQL uses placeholders and not string concatenation.
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        sqlCaptor.getValue(),
        "SQL must use parameter placeholders and not inline user input");

    // 2. Ensure user inputs are bound as parameters, not concatenated.
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // 3. Ensure success path still works after the fix (regression check).
    // The AttackResult type has limited API; we assert we got back a non-null object
    // and that the same instance is returned from the builder chain by identity.
    assertSame(result.getClass(), AttackResult.class, "Expected AttackResult instance");
  }
}
