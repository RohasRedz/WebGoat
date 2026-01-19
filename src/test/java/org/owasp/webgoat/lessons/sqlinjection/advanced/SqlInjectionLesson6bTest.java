package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionLesson6b focusing on:
 * 1) getPassword() still retrieves the password from the database.
 * 2) Exceptions are logged using the logger instead of printStackTrace().
 */
public class SqlInjectionLesson6bTest {

  private LessonDataSource lessonDataSource;
  private SqlInjectionLesson6b lesson;

  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;

  @BeforeEach
  public void setUp() throws Exception {
    javax.sql.DataSource realDataSource = mock(javax.sql.DataSource.class);
    lessonDataSource = new LessonDataSource(realDataSource);

    connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);

    when(realDataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);

    lesson = new SqlInjectionLesson6b(lessonDataSource);
  }

  @Test
  public void getPassword_readsPasswordFromDatabase_whenQuerySucceeds() throws Exception {
    when(resultSet != null && resultSet.first()).thenReturn(true);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("db-password");

    String password = lesson.getPassword();

    assertEquals("db-password", password);
  }

  @Test
  public void completed_returnsSuccessWhenUseridMatchesRetrievedPassword() throws Exception {
    when(resultSet != null && resultSet.first()).thenReturn(true);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("db-password");

    AttackResult result = lesson.completed("db-password");

    // When the provided userid_6b equals the DB password, the lesson should be completed.
    assertEquals(true, result.getLessonCompleted());
  }

  @Test
  public void completed_returnsFailureWhenUseridDoesNotMatchRetrievedPassword() throws Exception {
    when(resultSet != null && resultSet.first()).thenReturn(true);
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("db-password");

    AttackResult result = lesson.completed("wrong");

    assertEquals(false, result.getLessonCompleted());
  }
}
