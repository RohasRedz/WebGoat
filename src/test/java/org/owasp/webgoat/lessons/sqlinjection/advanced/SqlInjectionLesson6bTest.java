package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

class SqlInjectionLesson6bTest {

  private LessonDataSource dataSource;
  private SqlInjectionLesson6b lesson6b;

  @BeforeEach
  void setUp() {
    dataSource = mock(LessonDataSource.class);
    lesson6b = new SqlInjectionLesson6b(dataSource);
  }

  @Test
  void completed_succeedsWhenUserIdMatchesPasswordFromDatabase() throws Exception {
    // Arrange
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("securePasswordFromDb");

    // Act
    AttackResult result = lesson6b.completed("securePasswordFromDb");

    // Assert: main functional behavior still works after removing stack trace logging
    assert result != null;
    assert result.getLessonCompleted();
  }

  @Test
  void completed_failsWhenUserIdDoesNotMatchPasswordFromDatabase() throws Exception {
    // Arrange
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("securePasswordFromDb");

    // Act
    AttackResult result = lesson6b.completed("wrongPassword");

    // Assert: still fails as before; removal of printStackTrace does not affect logic
    assert result != null;
    assert !result.getLessonCompleted();
  }
}
