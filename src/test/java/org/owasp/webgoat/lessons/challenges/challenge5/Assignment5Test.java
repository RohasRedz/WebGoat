package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on the change from string-concatenated SQL to
 * parameterized PreparedStatement to prevent SQL injection.
 */
public class Assignment5Test {

  @Test
  void login_usesParameterizedQueryAndBindsUserInput() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement("select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    AttackResult result = assignment5.login("Larry", "pass' OR '1'='1");

    // Verify that user input is bound as parameters, not concatenated into SQL
    verify(preparedStatement).setString(1, "Larry");
    verify(preparedStatement).setString(2, "pass' OR '1'='1");
    verify(preparedStatement).executeQuery();

    assertTrue(result.getLessonCompleted(), "AttackResult should indicate challenge solved for correct credentials");

    // Ensure SQL text does not contain raw user input
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();
    assertFalse(usedSql.contains("Larry"));
    assertFalse(usedSql.contains("pass' OR '1'='1"));
  }

  @Test
  void login_failsForNonLarryUser() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    AttackResult result = assignment5.login("Bob", "anything");

    assertFalse(result.getLessonCompleted(), "Non-Larry user should not solve the challenge");
  }
}
