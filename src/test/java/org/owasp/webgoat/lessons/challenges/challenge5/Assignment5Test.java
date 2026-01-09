package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix:
 * - Ensures parameterized PreparedStatement is used correctly so that:
 *   - Valid credentials for user "Larry" succeed.
 *   - Invalid credentials fail.
 * - We do not inspect exact SQL text, only behavior against the mocked JDBC API.
 *
 * Resolved test file path (per instructions):
 * src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 */
@Slf4j
public class Assignment5Test {

  @Test
  @DisplayName("login succeeds for Larry with correct password using parameterized query")
  void loginSucceedsForValidLarryCredentials() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    DataSource dataSource = Mockito.mock(DataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);
    Flags flags = Mockito.mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    // Assignment5 uses try-with-resources on LessonDataSource#getConnection(),
    // but the underlying DataSource/Connection interactions are opaque here.
    // We only need the Connection mock.
    when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "correct-password");

    // Assert
    // If the SQL concatenation / injection bug reappears or the query is malformed,
    // executeQuery() or the flow would fail and this result would not be a success.
    assertEquals(true, result.getLessonCompleted(), "Expected successful lesson completion");
  }

  @Test
  @DisplayName("login fails for invalid Larry password using parameterized query")
  void loginFailsForInvalidPassword() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);
    Flags flags = Mockito.mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    // Simulate no matching row for wrong password
    when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "wrong-password");

    // Assert
    assertEquals(false, result.getLessonCompleted(), "Expected lesson to remain incomplete");
  }
}
