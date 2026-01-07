package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on removal of hard-coded password
 * and reliance on database value only.
 *
 * Behavior under test:
 * - getPassword() no longer returns the hard-coded value "dave" when the DB returns nothing.
 * - When the DB query returns a row, getPassword() returns the DB value.
 */
public class SqlInjectionLesson6bTest {

  private LessonDataSource dataSource;
  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;
  private SqlInjectionLesson6b lesson6b;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    connection = org.mockito.Mockito.mock(Connection.class);
    statement = org.mockito.Mockito.mock(Statement.class);
    resultSet = org.mockito.Mockito.mock(ResultSet.class);
    lesson6b = new SqlInjectionLesson6b(dataSource);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            org.mockito.Mockito.eq(ResultSet.TYPE_SCROLL_INSENSITIVE),
            org.mockito.Mockito.eq(ResultSet.CONCUR_READ_ONLY)))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
  }

  @Test
  void getPassword_returnsDatabasePasswordWhenRowExists() throws Exception {
    when(resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("db-secret");

    String password = lesson6b.getPassword();

    // After the fix, the method must still return the DB value when available.
    assertEquals("db-secret", password);
  }

  @Test
  void getPassword_doesNotFallBackToHardCodedDefaultWhenNoRow() throws Exception {
    // Simulate no results from the database
    when(resultSet.first()).thenReturn(false);

    String password = lesson6b.getPassword();

    // Before the fix, this would have returned "dave" (hard-coded default).
    // After the fix, the default is an empty string, eliminating the hard-coded secret.
    assertEquals("", password);
  }
}
