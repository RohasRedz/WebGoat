package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on the parameterized query behavior:
 * - Success only for correct credentials.
 * - SQL-injection-like input must not bypass authentication.
 */
public class Assignment5Test {

  private LessonDataSource lessonDataSource;
  private DataSource realDataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private Flags flags;
  private Assignment5 assignment5;

  @BeforeEach
  void setUp() throws Exception {
    // Basic sanity check to ensure Spring's StringUtils is available, as in production code.
    StringUtils.hasText("init");

    // Mock the JDBC stack used by LessonDataSource
    realDataSource = Mockito.mock(DataSource.class);
    lessonDataSource = new LessonDataSource(realDataSource);
    connection = Mockito.mock(Connection.class);
    preparedStatement = Mockito.mock(PreparedStatement.class);
    resultSet = Mockito.mock(ResultSet.class);
    when(realDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    // Mock flags
    flags = Mockito.mock(Flags.class);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    assignment5 = new Assignment5(lessonDataSource, flags);
  }

  @Test
  void loginSucceedsForCorrectCredentials() throws Exception {
    // Arrange
    when(resultSet.next()).thenReturn(true);

    // Act
    AttackResult result = assignment5.login("Larry", "correctPassword");

    // Assert
    assertEquals(AttackResult.Status.SUCCESS, result.getLessonStatus());
    Mockito.verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    Mockito.verify(preparedStatement).setString(1, "Larry");
    Mockito.verify(preparedStatement).setString(2, "correctPassword");
  }

  @Test
  void sqlInjectionLikePasswordDoesNotBypassAuthentication() throws Exception {
    // Arrange
    when(resultSet.next()).thenReturn(false);
    String maliciousPassword = "' OR '1'='1";

    // Act
    AttackResult result = assignment5.login("Larry", maliciousPassword);

    // Assert
    assertEquals(AttackResult.Status.FAIL, result.getLessonStatus());
    Mockito.verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    Mockito.verify(preparedStatement).setString(1, "Larry");
    Mockito.verify(preparedStatement).setString(2, maliciousPassword);
  }
}
