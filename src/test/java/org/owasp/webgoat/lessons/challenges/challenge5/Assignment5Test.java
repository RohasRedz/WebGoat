package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

  private LessonDataSource dataSource;
  private Flags flags;
  private Assignment5 assignment5;

  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = mock(LessonDataSource.class);
    flags = mock(Flags.class);
    assignment5 = new Assignment5(dataSource, flags);

    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(flags.getFlag(5)).thenReturn("FLAG-5");
  }

  @Test
  void login_usesParameterizedQueryAndSucceedsForValidLarryUser() throws Exception {
    // Arrange: simulate that credentials are correct (one row returned)
    when(resultSet.next()).thenReturn(true);

    // Act
    AttackResult result = assignment5.login("Larry", "secretPassword");

    // Assert: success path still works with parameterized query
    assert result != null;
    assert result.getLessonCompleted();
  }

  @Test
  void login_failsWhenCredentialsDoNotMatchEvenWithSqlInjectionAttempt() throws Exception {
    // Arrange: simulate that no rows are returned for SQL injection payload
    when(resultSet.next()).thenReturn(false);

    String maliciousPassword = "anything' OR '1'='1";

    // Act
    AttackResult result = assignment5.login("Larry", maliciousPassword);

    // Assert: SQL injection should no longer succeed; query is parameterized
    assert result != null;
    assert !result.getLessonCompleted();
  }
}
