package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

/**
 * Delta tests for Assignment5 focusing on the SQL-injection fix:
 * - Ensure PreparedStatement with placeholders is used.
 * - Ensure user input is passed as parameters (not concatenated into SQL).
 *
 * These tests use a mock LessonDataSource that exposes the underlying JDBC objects so we can
 * assert parameter binding behavior without inspecting private fields.
 */
class Assignment5Test {

  private Assignment5 assignment5;
  private Flags flags;
  private TrackingLessonDataSource trackingLessonDataSource;

  @BeforeEach
  void setUp() throws Exception {
    flags = mock(Flags.class);
    when(flags.getFlag(5)).thenReturn("FLAG-5");
    trackingLessonDataSource = new TrackingLessonDataSource();
    assignment5 = new Assignment5(trackingLessonDataSource, flags);
  }

  @Test
  void login_usesPreparedStatementAndBindsUserInputs() throws Exception {
    String username = "Larry";
    String password = "anyPassword' OR '1'='1";

    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true);

    PreparedStatement ps = mock(PreparedStatement.class);
    when(ps.executeQuery()).thenReturn(rs);

    Connection connection = mock(Connection.class);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(ps);

    trackingLessonDataSource.setConnection(connection);

    AttackResult result = assignment5.login(username, password);

    // Verify secure binding: user data is set as parameters, not concatenated.
    verify(connection)
        .prepareStatement(
            "select password from challenge_users where userid = ? and password = ?");
    verify(ps).setString(1, username);
    verify(ps).setString(2, password);
    verify(ps).executeQuery();

    assertTrue(result.getOutput().contains("challenge.solved"));
  }

  @Test
  void login_stillRejectsNonLarryUserEvenWithInjectionPayload() throws Exception {
    String username = "Mallory' OR '1'='1";
    String password = "anything";

    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(false);

    PreparedStatement ps = mock(PreparedStatement.class);
    when(ps.executeQuery()).thenReturn(rs);

    Connection connection = mock(Connection.class);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(ps);

    trackingLessonDataSource.setConnection(connection);

    AttackResult result = assignment5.login(username, password);

    // Because of the Larry check, we should fail before query execution.
    // The parameter binding behavior is still the fixed behavior, but the primary assertion here
    // is that the attack does not succeed.
    assertFalse(result.getOutput().contains("challenge.solved"));
    assertTrue(result.getOutput().contains("user.not.larry"));
  }

  /**
   * Simple wrapper around a DataSource to allow injecting a mocked Connection. This stays within
   * the same package/imports used by the updated class.
   */
  private static class TrackingLessonDataSource implements LessonDataSource {

    private Connection connection;

    void setConnection(Connection connection) {
      this.connection = connection;
    }

    @Override
    public Connection getConnection() {
      return connection;
    }

    @Override
    public DataSource getDataSource() {
      return null;
    }
  }
}
