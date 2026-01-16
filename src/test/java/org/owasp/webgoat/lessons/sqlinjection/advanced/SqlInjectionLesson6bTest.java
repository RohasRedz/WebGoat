package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword should handle SQLExceptions and return default password")
  void getPassword_handlesSqlException_andReturnsDefault() throws Exception {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery(anyString())).thenThrow(new SQLException("boom"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    String password = lesson.getPassword();

    assertEquals("dave", password);
  }
}
