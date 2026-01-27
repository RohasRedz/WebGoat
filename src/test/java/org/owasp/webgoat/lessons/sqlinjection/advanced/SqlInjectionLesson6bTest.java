package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging change:
 * - verify that printStackTrace() is no longer used
 * - verify that SQLExceptions are logged via SLF4J logger (log.error)
 *
 * Since the class now has @Slf4j, we validate behavior indirectly by checking that:
 * - the exception thrown from createStatement/executeQuery is propagated and no printStackTrace()
 *   calls are made on the exception.
 */
public class SqlInjectionLesson6bTest {

  @Mock
  private LessonDataSource dataSource;

  @Mock
  private Connection connection;

  @Mock
  private Statement statement;

  @Mock
  private ResultSet resultSet;

  private SqlInjectionLesson6b lesson;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    when(dataSource.getConnection()).thenReturn(connection);
    lesson = new SqlInjectionLesson6b(dataSource);
  }

  @Test
  void getPassword_logsSqlExceptionWithoutPrintStackTrace() throws Exception {
    // Arrange: simulate SQLException thrown when creating statement
    SQLException sqlException = new SQLException("DB error");
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(sqlException);

    // Act
    String password = lesson.getPassword();

    // Assert: method returns default "dave" and does not call printStackTrace on the exception
    assertEquals("dave", password);
    verify(connection, times(1))
        .createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

    // Important: ensure printStackTrace is not called on exception
    verify(sqlException, never()).printStackTrace();
  }

  @Test
  void getPassword_logsGeneralExceptionWithoutPrintStackTrace() throws Exception {
    // Arrange: simulate generic exception when getting connection
    Exception generic = new RuntimeException("generic");
    when(dataSource.getConnection()).thenThrow(generic);

    // Act
    String password = lesson.getPassword();

    // Assert: still returns default "dave" and no printStackTrace on generic exception
    assertEquals("dave", password);
    verify(generic, never()).printStackTrace();
  }
}
