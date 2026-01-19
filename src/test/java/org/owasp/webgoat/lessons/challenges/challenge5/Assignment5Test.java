package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing only on the changed SQL behavior:
 * - Ensure a PreparedStatement with parameter placeholders is used.
 * - Ensure user input is bound via setString (no concatenated SQL).
 */
public class Assignment5Test {

  private LessonDataSource lessonDataSource;
  private DataSource delegateDataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private Flags flags;

  @BeforeEach
  void setUp() throws Exception {
    delegateDataSource = mock(DataSource.class);
    lessonDataSource = new LessonDataSource(delegateDataSource);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    flags = mock(Flags.class);

    org.mockito.Mockito.when(delegateDataSource.getConnection()).thenReturn(connection);
    org.mockito.Mockito.when(
            connection.prepareStatement(
                org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(preparedStatement);
    org.mockito.Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }

  @Test
  void login_usesParameterizedQueryAndBindsUserInputs() throws Exception {
    String username = "Larry";
    String password = "secretPass";
    org.mockito.Mockito.when(resultSet.next()).thenReturn(true);
    org.mockito.Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    AttackResult result = assignment5.login(username, password);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());

    String usedSql = sqlCaptor.getValue();
    assertTrue(
        usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
        "SQL should use parameter placeholders instead of concatenating user input");

    verify(preparedStatement).setString(eq(1), eq(username));
    verify(preparedStatement).setString(eq(2), eq(password));

    assertTrue(result.isLessonCompleted(), "Successful login should still complete the lesson");
  }

  @Test
  void login_failsWhenSqlNotParameterized_likeOldConcatenation() throws Exception {
    String username = "Larry' OR '1'='1";
    String password = "anything";
    org.mockito.Mockito.when(resultSet.next()).thenReturn(false);

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    AttackResult result = assignment5.login(username, password);

    assertTrue(
        !result.isLessonCompleted(),
        "SQL injection style username should not bypass authentication after fix");
  }

  @Test
  void login_rejectsEmptyInputConsistently() throws Exception {
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    AttackResult result = assignment5.login("", "");

    assertTrue(
        !StringUtils.hasText(""),
        "Spring's StringUtils.hasText should treat empty string as invalid");
    assertTrue(!result.isLessonCompleted(), "Empty input must still be rejected");
  }
}
