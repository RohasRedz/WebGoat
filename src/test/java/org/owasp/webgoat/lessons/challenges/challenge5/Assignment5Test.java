package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

// NOTE: This is a delta test focusing specifically on the SQL injection fix:
// the code now uses parameterized queries instead of string concatenation.
public class Assignment5Test {

  private LessonDataSource lessonDataSource;
  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private Flags flags;
  private Assignment5 assignment5;

  @BeforeEach
  void setUp() throws Exception {
    lessonDataSource = Mockito.mock(LessonDataSource.class);
    dataSource = Mockito.mock(DataSource.class);
    connection = Mockito.mock(Connection.class);
    preparedStatement = Mockito.mock(PreparedStatement.class);
    resultSet = Mockito.mock(ResultSet.class);
    flags = Mockito.mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);

    assignment5 = new Assignment5(lessonDataSource, flags);
  }

  @Test
  void login_usesParameterizedQuery_whenValidLarryCredentialsProvided() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "secret";
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG5");

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Core delta behavior: parameters must be bound via setString,
    // so that the query is not built via string concatenation.
    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, password);
    Mockito.verify(preparedStatement).executeQuery();

    // And the functional behavior (challenge solved) is preserved.
    assertEquals("success", result.getType().toString().toLowerCase());
  }

  @Test
  void login_treatsInjectionPayloadAsData_notAsExecutableSql() throws Exception {
    // Arrange
    String maliciousPassword = "anything' OR '1'='1";
    String username = "Larry";

    // Simulate that no row is returned when the malicious payload is used.
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    // Act
    AttackResult result = assignment5.login(username, maliciousPassword);

    // Assert
    // The malicious payload must be bound as a parameter rather than altering SQL structure.
    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, maliciousPassword);

    assertEquals("failure", result.getType().toString().toLowerCase());
  }

  @Test
  void login_failsWhenParametersAreEmpty_asBefore() throws Exception {
    // Arrange
    String username = "";
    String password = "";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Existing behavior for empty input must be preserved.
    assertEquals("failure", result.getType().toString().toLowerCase());
  }
}
