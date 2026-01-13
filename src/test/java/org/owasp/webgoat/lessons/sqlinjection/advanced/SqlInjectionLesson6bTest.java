package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on safer logging instead of printStackTrace().
 *
 * File under test:
 * src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6b.java
 */
class SqlInjectionLesson6bTest {

  /**
   * Simple wrapper subclass to allow injecting a mock logger if needed in the future.
   * Currently we assert behavior indirectly by triggering the catch blocks.
   */
  static class SqlInjectionLesson6bTestable extends SqlInjectionLesson6b {
    SqlInjectionLesson6bTestable(LessonDataSource dataSource) {
      super(dataSource);
    }
  }

  @Test
  @DisplayName("getPassword() should log errors via SLF4J and not use printStackTrace")
  void getPasswordUsesSlf4jLoggingInsteadOfPrintStackTrace() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);

    // Force a SQLException from executeQuery to exercise the inner catch block
    Mockito.when(lessonDataSource.getConnection()).thenReturn(connection);
    Mockito.when(
            connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    Mockito.when(statement.executeQuery(Mockito.anyString()))
        .thenThrow(new java.sql.SQLException("boom"));

    SqlInjectionLesson6bTestable lesson = new SqlInjectionLesson6bTestable(lessonDataSource);

    // Spy on the logger via Mockito's inline mocking of static SLF4J logger field
    // We indirectly verify that an error log with our message is produced.
    org.slf4j.Logger logger = mock(org.slf4j.Logger.class);
    java.lang.reflect.Field loggerField =
        SqlInjectionLesson6b.class.getDeclaredField("log");
    loggerField.setAccessible(true);
    loggerField.set(null, logger);

    // Act
    lesson.getPassword();

    // Assert
    // Delta assertion: an error should be logged instead of using printStackTrace
    verify(logger)
        .error(
            contains("SQL Exception in getPassword"),
            Mockito.anyString());
  }
}
