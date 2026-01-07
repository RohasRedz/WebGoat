package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta/security-focused tests for SqlInjectionLesson6b.
 *
 * Focus:
 * - getPassword() should use the database value when available.
 * - getPassword() must no longer fall back to the old hard-coded default ("dave")
 *   when the DB call fails or returns no rows.
 */
public class SqlInjectionLesson6bSecurityTest {

  @Test
  @DisplayName("getPassword returns database password when query succeeds")
  void getPasswordReturnsDbValueWhenAvailable() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(
            connection.createStatement(
                Mockito.eq(ResultSet.TYPE_SCROLL_INSENSITIVE),
                Mockito.eq(ResultSet.CONCUR_READ_ONLY)))
        .thenReturn(statement);
    Mockito.when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
    Mockito.when(resultSet.first()).thenReturn(true);
    Mockito.when(resultSet.getString("password")).thenReturn("db-secret-password");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    assertEquals(
        "db-secret-password",
        password,
        "getPassword should return the database password when query returns a row");
  }

  @Test
  @DisplayName("getPassword does not fall back to hard-coded default when DB returns no rows")
  void getPasswordIsNullWhenNoDbRow() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(
            connection.createStatement(
                Mockito.eq(ResultSet.TYPE_SCROLL_INSENSITIVE),
                Mockito.eq(ResultSet.CONCUR_READ_ONLY)))
        .thenReturn(statement);
    Mockito.when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
    // Simulate query executed but no rows returned
    Mockito.when(resultSet.first()).thenReturn(false);

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    // Before the fix this would have been the default "dave"; now it must not be.
    assertNull(
        password,
        "When DB returns no rows, getPassword should not fall back to any hard-coded default");
  }

  @Test
  @DisplayName("getPassword does not expose hard-coded default when DB throws exception")
  void getPasswordDoesNotExposeDefaultOnException() throws Exception {
    // Arrange
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    // Simulate exception when obtaining a connection
    Mockito.when(dataSource.getConnection()).thenThrow(new RuntimeException("DB down"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Act
    String password = lesson.getPassword();

    // Assert
    // Prior to fix, this might have returned "dave"; now null is expected (no hard-coded fallback).
    assertNull(
        password,
        "When DB connection fails, getPassword should not return any hard-coded default password");
  }
}
