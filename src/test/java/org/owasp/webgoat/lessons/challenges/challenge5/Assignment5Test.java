package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.owasp.webgoat.container.assignments.AttackResult.Status.SUCCESS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests focused on the SQL parameterization change in Assignment5.login.
 *
 * This test file is intended to live at:
 * src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 */
public class Assignment5Test {

  private LessonDataSource lessonDataSource;
  private DataSource delegateDataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private Flags flags;
  private Assignment5 assignment5;

  @BeforeEach
  void setUp() throws Exception {
    delegateDataSource = Mockito.mock(DataSource.class);
    lessonDataSource = Mockito.mock(LessonDataSource.class);
    connection = Mockito.mock(Connection.class);
    preparedStatement = Mockito.mock(PreparedStatement.class);
    resultSet = Mockito.mock(ResultSet.class);
    flags = Mockito.mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    assignment5 = new Assignment5(lessonDataSource, flags);
  }

  @Test
  void login_usesParameterizedQueryAndBindsUserInputs() throws Exception {
    String username = "Larry";
    String password = "password123";

    when(resultSet.next()).thenReturn(true);

    AttackResult result = assignment5.login(username, password);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();

    // Assert that the SQL now uses parameter placeholders instead of concatenating user input.
    org.junit.jupiter.api.Assertions.assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        usedSql);

    // Verify that the user inputs are bound via setString on the PreparedStatement.
    Mockito.verify(preparedStatement).setString(1, username);
    Mockito.verify(preparedStatement).setString(2, password);

    // Ensure the rest of the behavior (successful login) is preserved.
    assertEquals(SUCCESS, result.getStatus());
  }

  @Test
  void login_doesNotConcatenateMaliciousInputIntoSql() throws Exception {
    String username = "Larry";
    String password = "abc' OR '1'='1";

    when(resultSet.next()).thenReturn(false);

    assignment5.login(username, password);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();

    // Ensure the query template is fixed and does not contain attacker-controlled fragments.
    org.junit.jupiter.api.Assertions.assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        usedSql);

    // The malicious password must only appear as a bound parameter, not in the SQL string.
    org.junit.jupiter.api.Assertions.assertFalse(usedSql.contains(password));
  }
}
