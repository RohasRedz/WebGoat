package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on behavior after removing printStackTrace.
 *
 * Derived test path (per requirements):
 *   src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
 */
public class SqlInjectionLesson6bTest {

  private LessonDataSource dataSource;
  private SqlInjectionLesson6b lesson;

  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = mock(LessonDataSource.class);
    lesson = new SqlInjectionLesson6b(dataSource);

    connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            Mockito.eq(ResultSet.TYPE_SCROLL_INSENSITIVE),
            Mockito.eq(ResultSet.CONCUR_READ_ONLY)))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
  }

  @Test
  void getPassword_returnsPasswordFromResultSetWhenPresent() throws Exception {
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("secure-pass-from-db");

    String password = lesson.getPassword();

    assertEquals(
        "secure-pass-from-db",
        password,
        "getPassword should still return the password from the result set after logging changes");
  }

  @Test
  void getPassword_returnsDefaultWhenResultSetEmpty() throws Exception {
    when(resultSet.first()).thenReturn(false);

    String password = lesson.getPassword();

    // When no row is returned, the default "dave" is used and should remain unchanged.
    assertEquals(
        "dave",
        password,
        "Behavior with no DB result must remain unchanged even after printStackTrace removal");
  }
}
