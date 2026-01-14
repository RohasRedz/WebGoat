// File: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

class Assignment5Test {

  @Test
  void login_usesPreparedStatementParametersAndSucceedsForValidLarry() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    try (MockedStatic<StringUtils> stringUtilsMock = mockStatic(StringUtils.class)) {
      stringUtilsMock
          .when(() -> StringUtils.hasText("Larry"))
          .thenReturn(true);
      stringUtilsMock
          .when(() -> StringUtils.hasText("secret"))
          .thenReturn(true);

      AttackResult result = assignment5.login("Larry", "secret");

      verify(connection, times(1))
          .prepareStatement(
              "select password from challenge_users where userid = ? and password = ?");
      verify(preparedStatement, times(1)).setString(1, "Larry");
      verify(preparedStatement, times(1)).setString(2, "secret");

      assertEquals("FLAG-5", result.getFeedbackArgs()[0]);
    }
  }

  @Test
  void login_failsWhenNotLarryWithoutChangingQueryShape() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    try (MockedStatic<StringUtils> stringUtilsMock = mockStatic(StringUtils.class)) {
      stringUtilsMock
          .when(() -> StringUtils.hasText("Bob"))
          .thenReturn(true);
      stringUtilsMock
          .when(() -> StringUtils.hasText("secret"))
          .thenReturn(true);

      AttackResult result = assignment5.login("Bob", "secret");

      // Ensure that when username is not Larry, the database is not called at all
      verify(dataSource, times(0)).getConnection();
      // The secure behavior (no concatenation) is implicitly validated by the first test;
      // this test ensures behavior for non-Larry is unchanged.
      assertEquals("user.not.larry", result.getFeedbackId());
    }
  }
}
