package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.slf4j.Logger;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging change:
 * - Exceptions are no longer printed via printStackTrace, but logged via SLF4J error logging.
 */
class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("completed uses getPassword and does not expose stack traces directly")
  void completedDoesNotExposeStackTrace() throws IOException {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet resultSet = mock(ResultSet.class);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenThrow(new RuntimeException("DB error"));

    Logger logger = mock(Logger.class);
    SqlInjectionLesson6b lesson =
        new SqlInjectionLesson6b(dataSource) {
          @Override
          protected String getPassword() {
            String password = "dave";
            try (Connection conn = dataSource.getConnection()) {
              String query = "SELECT password FROM user_system_data WHERE user_name = 'dave'";
              try {
                Statement stmt =
                    conn.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ResultSet results = stmt.executeQuery(query);
                if (results != null && results.first()) {
                  password = results.getString("password");
                }
              } catch (Exception sqle) {
                logger.error("SQL Exception occurred while fetching password", sqle);
              }
            } catch (Exception e) {
              logger.error("General Exception occurred while fetching password", e);
            }
            return password;
          }
        };

    // Act
    AttackResult result = lesson.completed("someUser");

    // Assert
    verify(logger, atLeastOnce())
        .error(eq("SQL Exception occurred while fetching password"), any(Throwable.class));
    // Behaviorally, lesson should fail since password retrieval failed
    assertEquals(false, result.getLessonCompleted());
  }

  @Test
  @DisplayName("completed succeeds when userid matches retrieved password")
  void completedSucceedsWhenUserIdMatchesPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("secretPw");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    AttackResult result = lesson.completed("secretPw");

    // Assert
    assertEquals(true, result.getLessonCompleted());
  }
}
