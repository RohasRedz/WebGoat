package org.owasp.webgoat.lessons.sqlinjection.advanced;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on the information exposure and
 * hardcoded-password fixes:
 * - Ensure getPassword() no longer returns a hardcoded default when the query fails.
 * - Ensure completed() behaves securely when the internal lookup fails (no unintended success).
 */
public class SqlInjectionLesson6bTest {

  private LessonDataSource dataSource;
  private SqlInjectionLesson6b lesson;

  private Connection connection;
  private Statement statement;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = Mockito.mock(LessonDataSource.class);
    lesson = new SqlInjectionLesson6b(dataSource);

    connection = Mockito.mock(Connection.class);
    statement = Mockito.mock(Statement.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
  }

  @Test
  void getPassword_returnsEmptyStringWhenQueryFails_insteadOfHardcodedSecret() throws Exception {
    // Arrange: simulate a failure when creating or executing the statement
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new RuntimeException("DB not available"));

    // Act
    String password = lesson.getPassword();

    // Assert: previously this would have returned the hardcoded "dave"
    assertEquals("", password, "getPassword should no longer return a hardcoded default value");
  }

  @Test
  void completed_doesNotSucceedWhenInternalPasswordLookupFails() throws IOException {
    // Arrange: force getPassword() to return empty (no DB row / failure)
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new RuntimeException("DB not available"));

    // Act: user attempts to complete lesson with arbitrary input
    AttackResult result = lesson.completed("dave");

    // Assert: since getPassword() will not fall back to \"dave\" anymore, the lesson must not succeed
    assertFalse(result.isSuccess(), "Lesson should not succeed when password lookup fails");
  }
}
