package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on the fixed, parameterized SQL behavior:
 * - Valid credential path still succeeds.
 * - Malicious input is treated as data and not as part of the SQL command.
 */
class Assignment5Test {

  @Test
  @DisplayName("login succeeds for Larry with correct password using parameterized query")
  void loginSucceedsForValidUser() throws Exception {
    // Arrange
    DataSource ds = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(ds.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);

    LessonDataSource lessonDataSource = new LessonDataSource(ds);
    Flags flags = mock(Flags.class);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "secret");

    // Assert
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(ps).setString(1, "Larry");
    verify(ps).setString(2, "secret");
    verify(ps).executeQuery();
    assertEquals(true, result.getLessonCompleted());
  }

  @Test
  @DisplayName("login rejects SQL injection payload by treating it as data in parameterized query")
  void loginRejectsSqlInjectionPayload() throws Exception {
    // Arrange
    DataSource ds = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(ds.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    // Simulate no matching row for injection payload
    when(rs.next()).thenReturn(false);

    LessonDataSource lessonDataSource = new LessonDataSource(ds);
    Flags flags = mock(Flags.class);

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String maliciousPassword = "anything' OR '1'='1";

    // Act
    AttackResult result = assignment5.login("Larry", maliciousPassword);

    // Assert
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(ps).setString(1, "Larry");
    verify(ps).setString(2, maliciousPassword);
    verify(ps).executeQuery();
    // Because the result set has no rows, the login must fail
    assertEquals(false, result.getLessonCompleted());
  }

  @Test
  @DisplayName("login fails when username is not Larry even with correct password")
  void loginFailsForNonLarryUser() throws Exception {
    // Arrange
    DataSource ds = mock(DataSource.class);
    LessonDataSource lessonDataSource = new LessonDataSource(ds);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult result = assignment5.login("Bob", "secret");

    // Assert
    assertEquals(false, result.getLessonCompleted());
  }
}
