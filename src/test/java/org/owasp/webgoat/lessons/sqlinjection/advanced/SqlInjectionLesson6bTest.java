package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the fixed exception handling behavior
 * that avoids leaking stack traces while keeping getPassword resilient.
 *
 * Test file path (derived): src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
 */
class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword returns non-null even when DB throws SQLException (no stack trace leak)")
  void getPassword_returnsNonNullWhenDbFails() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(
            connection.createStatement(
                Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(statement);
    Mockito.when(statement.executeQuery(Mockito.anyString()))
        .thenThrow(new SQLException("Simulated DB failure"));

    SqlInjectionLesson6b lesson6b = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson6b.getPassword();

    // Assert
    assertNotNull(password, "Password should not be null even when DB interaction fails");
  }

  @Test
  @DisplayName("completed does not propagate DB exceptions from getPassword")
  void completed_doesNotThrowWhenDbFailsInsideGetPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(
            connection.createStatement(
                Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(statement);
    Mockito.when(statement.executeQuery(Mockito.anyString()))
        .thenThrow(new SQLException("Simulated DB failure"));

    SqlInjectionLesson6b lesson6b = new SqlInjectionLesson6b(dataSource);

    // Act & Assert
    // The method should handle internal exceptions and not throw IOException/Runtime
    lesson6b.completed("anyUser");
  }
}
