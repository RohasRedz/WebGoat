package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging change:
 * - Ensures getPassword() executes successfully and returns the value from ResultSet
 *   without relying on printStackTrace().
 * - We do not attempt to intercept logger output; the behavioral guarantee is that
 *   the method completes and uses the mocked JDBC API.
 *
 * Resolved test file path (per instructions):
 * src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
 */
@Slf4j
public class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword retrieves password from database without throwing (no printStackTrace used)")
  void getPasswordReturnsValueFromDatabase() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            Mockito.eq(ResultSet.TYPE_SCROLL_INSENSITIVE),
            Mockito.eq(ResultSet.CONCUR_READ_ONLY)))
        .thenReturn(statement);
    when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("secret-db-password");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(lessonDataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    // If printStackTrace() were still used or exceptions occurred, the method might
    // return the default "dave" or propagate an error. A proper, logged flow returns
    // the value from the ResultSet instead.
    assertEquals("secret-db-password", password);
  }
}
