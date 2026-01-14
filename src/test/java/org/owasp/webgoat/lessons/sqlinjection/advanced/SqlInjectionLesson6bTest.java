package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b, focused on the logging / information exposure fix:
 * verifying that getPassword() no longer throws when an exception occurs and that
 * completed() still behaves correctly based on the returned password.
 *
 * Note: We do not assert on logging output; we only ensure the method remains callable
 * and that no stack traces escape via thrown exceptions.
 */
public class SqlInjectionLesson6bTest {

  private LessonDataSource dataSource;
  private SqlInjectionLesson6b lesson;

  @BeforeEach
  void setUp() {
    dataSource = mock(LessonDataSource.class);
    lesson = new SqlInjectionLesson6b(dataSource);
  }

  @Test
  void completed_succeedsWhenUserIdMatchesPasswordFromGetPassword() throws IOException {
    // Arrange
    SqlInjectionLesson6b spyLesson = new SqlInjectionLesson6b(dataSource) {
      @Override
      protected String getPassword() {
        return "secret";
      }
    };

    // Act
    var result = spyLesson.completed("secret");

    // Assert
    // Ensure behavior is preserved when userid_6b equals the password.
    assertEquals("success", result.getType().name().toLowerCase());
  }

  @Test
  void completed_failsWhenUserIdDoesNotMatchPasswordFromGetPassword() throws IOException {
    // Arrange
    SqlInjectionLesson6b spyLesson = new SqlInjectionLesson6b(dataSource) {
      @Override
      protected String getPassword() {
        return "secret";
      }
    };

    // Act
    var result = spyLesson.completed("wrong-user");

    // Assert
    assertEquals("failure", result.getType().name().toLowerCase());
  }

  @Test
  void getPassword_doesNotThrowWhenDataSourceConnectionFails() {
    // Arrange
    // Force dataSource.getConnection() to throw, simulating a DB connectivity issue.
    LessonDataSource failingDataSource = mock(LessonDataSource.class);
    when(failingDataSource.getConnection()).thenThrow(new RuntimeException("DB down"));
    SqlInjectionLesson6b failingLesson = new SqlInjectionLesson6b(failingDataSource);

    // Act
    String password = failingLesson.getPassword();

    // Assert
    // The fix replaced printStackTrace() with logging; we verify that no exception escapes
    // and that some (fallback) password string is returned.
    // The exact value is not important here; we just assert that it is non-null.
    org.junit.jupiter.api.Assertions.assertNotNull(password);
  }
}
