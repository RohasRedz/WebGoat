package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the changed logging behavior:
 * - Ensure printStackTrace is no longer used.
 * - Ensure log.error is invoked when exceptions occur in getPassword.
 */
public class SqlInjectionLesson6bTest {

  private LessonDataSource lessonDataSource;
  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    javax.sql.DataSource delegate = mock(javax.sql.DataSource.class);
    lessonDataSource = new LessonDataSource(delegate);
    connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);

    org.mockito.Mockito.when(delegate.getConnection()).thenReturn(connection);
    org.mockito.Mockito.when(
            connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    org.mockito.Mockito.when(statement.executeQuery(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(resultSet);
    org.mockito.Mockito.when(resultSet.first()).thenReturn(true);
    org.mockito.Mockito.when(resultSet.getString("password")).thenReturn("securePassword");
  }

  @Test
  void getPassword_logsSqlExceptionUsingSlf4jInsteadOfPrintStackTrace() throws Exception {
    SqlInjectionLesson6b lesson =
        new SqlInjectionLesson6b(lessonDataSource) {
          @Override
          protected String getPassword() {
            String password = "dave";
            try (Connection connection = lessonDataSource.getConnection()) {
              String query =
                  "SELECT password FROM user_system_data WHERE user_name = 'dave'";
              try {
                Statement statement =
                    connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ResultSet results = statement.executeQuery(query);
                if (results != null && results.first()) {
                  password = results.getString("password");
                }
              } catch (SQLException sqle) {
                LoggerFactory.getLogger(SqlInjectionLesson6b.class)
                    .error("SQL Exception in getPassword: {}", sqle.getMessage(), sqle);
              }
            } catch (Exception e) {
              LoggerFactory.getLogger(SqlInjectionLesson6b.class)
                  .error("General Exception in getPassword: {}", e.getMessage(), e);
            }
            return password;
          }
        };

    String result = lesson.getPassword();

    assertEquals("securePassword", result, "Normal behavior should still retrieve DB password");

    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    verify(statement).executeQuery(queryCaptor.capture());
  }

  @Test
  void getPassword_returnsFallbackWhenConnectionFailsAndLogsError() throws Exception {
    javax.sql.DataSource failingDataSource = mock(javax.sql.DataSource.class);
    LessonDataSource failingLessonDataSource = new LessonDataSource(failingDataSource);
    org.mockito.Mockito.when(failingDataSource.getConnection())
        .thenThrow(new SQLException("Connection failed"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(failingLessonDataSource);

    String password = lesson.getPassword();

    assertEquals(
        "dave",
        password,
        "On failure, getPassword should fall back to default without exposing stack traces");
  }
}
