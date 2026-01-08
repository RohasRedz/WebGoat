package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on the fixed SQL injection behavior.
 *
 * Derived test path (per instructions):
 * src/main/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java
 * -> src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 */
public class Assignment5Test {

  private LessonDataSource lessonDataSource;
  private Flags flags;
  private Assignment5 assignment5;

  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    // We rely on the same imports as updated source where possible
    lessonDataSource = mock(LessonDataSource.class);
    flags = mock(Flags.class);
    assignment5 = new Assignment5(lessonDataSource, flags);

    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    when(flags.getFlag(5)).thenReturn("FLAG-5");
  }

  @Test
  void loginSucceedsOnlyWithCorrectCredentials() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "correct-password";

    when(resultSet.next()).thenReturn(true);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Ensure success is returned when DB says credentials are valid
    // and that prepared statement used parameter binding (no concatenation visible here).
    assertEquals(true, result.getLessonCompleted());

    // Verify the parameterized query usage: two positional parameters
    verify(connection)
        .prepareStatement("select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
  }

  @Test
  void sqlInjectionAttemptDoesNotBypassAuthentication() throws Exception {
    // Arrange
    String username = "Larry' OR '1'='1";
    String password = "anything";

    // Simulate database returning no row for this injected attempt
    when(resultSet.next()).thenReturn(false);

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Attack must fail; previously, concatenated SQL could succeed.
    assertEquals(false, result.getLessonCompleted());

    // Verify that even injection-like input is still passed as a literal parameter
    verify(connection)
        .prepareStatement("select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
  }
}
