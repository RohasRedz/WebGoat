package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
import org.springframework.util.StringUtils;

/**
 * Delta tests verifying that Assignment5.login() uses a parameterized PreparedStatement
 * instead of concatenating user input into the SQL query.
 */
public class Assignment5Test {

  private LessonDataSource lessonDataSource;
  private Flags flags;
  private Assignment5 assignment5;

  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @BeforeEach
  public void setUp() throws Exception {
    // Mock underlying JDBC objects and LessonDataSource so we can inspect how the query is built.
    DataSource realDataSource = mock(DataSource.class);
    lessonDataSource = new LessonDataSource(realDataSource);

    flags = mock(Flags.class);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(realDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    assignment5 = new Assignment5(lessonDataSource, flags);
  }

  @Test
  public void login_usesParameterizedQueryAndBindsUserInputs_correctCredentials() throws Exception {
    String username = "Larry";
    String password = "secret";

    when(resultSet.next()).thenReturn(true);

    AttackResult result = assignment5.login(username, password);

    // Verify parameters are bound via PreparedStatement instead of concatenated into SQL.
    // If the implementation regresses to string concatenation and does not call setString,
    // these calls will never occur and the test will fail.
    org.mockito.Mockito.verify(preparedStatement).setString(1, username);
    org.mockito.Mockito.verify(preparedStatement).setString(2, password);

    // Ensure the success path is still taken when the query returns a row.
    assertEquals("FLAG-5", result.getFeedbackArgs()[0]);
  }

  @Test
  public void login_usesParameterizedQueryAndFailsOnIncorrectCredentials() throws Exception {
    String username = "Larry";
    String password = "wrong";

    when(resultSet.next()).thenReturn(false);

    AttackResult result = assignment5.login(username, password);

    // Parameters should still be bound even when authentication fails.
    org.mockito.Mockito.verify(preparedStatement).setString(1, username);
    org.mockito.Mockito.verify(preparedStatement).setString(2, password);

    // Since credentials are incorrect, the attack result should indicate failure.
    // We only check the outcome type here; detailed feedback text is out of scope
    // for this delta test.
    assertEquals(false, result.getLessonCompleted());
  }
}
