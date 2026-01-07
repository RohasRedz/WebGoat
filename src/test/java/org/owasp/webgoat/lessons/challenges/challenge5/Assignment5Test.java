package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

  @Test
  @DisplayName(
      "login should authenticate valid user using parameterized query and reject SQL injection payloads")
  void login_usesParameterizedQuery_andRejectsSqlInjection() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String username = "Larry";
    String password = "password' OR '1'='1";

    AttackResult result = assignment5.login(username, password);

    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(eq(1), eq(username));
    verify(preparedStatement).setString(eq(2), eq(password));

    assertEquals(true, result.getLessonCompleted());
  }

  @Test
  @DisplayName("login should fail when username is not Larry, even with SQL injection payload")
  void login_rejectsNonLarryUser_withSqlInjectionPayload() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);

    Assignment5 assignment5 = new Assignment5(dataSource, flags);

    String maliciousUser = "Larry' OR '1'='1";
    String password = "doesnotmatter";

    AttackResult result = assignment5.login(maliciousUser, password);

    verifyNoInteractions(dataSource);
    assertEquals(false, result.getLessonCompleted());
  }
}
