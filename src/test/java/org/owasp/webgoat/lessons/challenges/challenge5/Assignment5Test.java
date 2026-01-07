package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix.
 *
 * Behavior under test:
 * - login() now uses a parameterized PreparedStatement with placeholders.
 * - User-supplied username and password must be bound via setString, not concatenated into SQL.
 */
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
    // Wire up mocks so we can verify parameter binding on the PreparedStatement.
    lessonDataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    dataSource = org.mockito.Mockito.mock(DataSource.class);
    connection = org.mockito.Mockito.mock(Connection.class);
    preparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
    resultSet = org.mockito.Mockito.mock(ResultSet.class);
    flags = org.mockito.Mockito.mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(org.mockito.Mockito.anyString()))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // LessonDataSource extends javax.sql.DataSource in WebGoat, so we can safely downcast here.
    // This allows the class under test to use the same getConnection() path.
    org.mockito.Mockito.when((dataSource).getConnection()).thenReturn(connection);

    assignment5 = new Assignment5(lessonDataSource, flags);
  }

  @Test
  void login_usesParameterizedPreparedStatement_andBindsUserInputs() throws Exception {
    String username = "Larry";
    String password = "password123";

    AttackResult result = assignment5.login(username, password);

    // Verify that PreparedStatement was created with placeholders, not with concatenated user input.
    verify(connection)
        .prepareStatement(
            eq("select password from challenge_users where userid = ? and password = ?"));

    // Verify that user-controlled values are bound using setString on the PreparedStatement.
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // Ensure that a successful login path is still functioning (behavior preserved).
    // The concrete message key is not asserted here to keep this test focused on the SQL fix.
    assertEquals(true, result.getLessonCompleted());
  }

  @Test
  void login_rejectsNonLarryUser_beforeQueryExecution() throws Exception {
    String username = "Mallory";
    String password = "anything";

    AttackResult result = assignment5.login(username, password);

    // For non-Larry users, the method should fail early and never prepare/execute a SQL query,
    // which further reduces attack surface.
    org.mockito.Mockito.verifyNoInteractions(connection);

    assertEquals(false, result.getLessonCompleted());
  }
}
