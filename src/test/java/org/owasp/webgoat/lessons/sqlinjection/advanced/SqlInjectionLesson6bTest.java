package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the changed logging behavior within getPassword:
 * - Verifies that exceptions are logged via SLF4J logger rather than using printStackTrace.
 * - Uses reflection to inject a mock logger to observe logging calls.
 */
public class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword logs SQLExceptions via SLF4J logger without throwing")
  void getPassword_logsSqlExceptionViaSlf4j() throws Exception {
    // Arrange
    LessonDataSource dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    Connection connection = org.mockito.Mockito.mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);

    // Force an SQLException from createStatement/executeQuery by throwing a runtime exception
    when(connection.createStatement(
            org.mockito.Mockito.anyInt(), org.mockito.Mockito.anyInt()))
        .thenThrow(new java.sql.SQLException("simulated SQL failure"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Inject mock logger into the Lombok-generated 'log' field via reflection
    org.slf4j.Logger mockLogger = org.mockito.Mockito.mock(org.slf4j.Logger.class);
    java.lang.reflect.Field logField = SqlInjectionLesson6b.class.getDeclaredField("log");
    logField.setAccessible(true);
    logField.set(null, mockLogger);

    // Act
    String password = lesson.getPassword();

    // Assert
    // Default password should still be returned despite the exception
    org.junit.jupiter.api.Assertions.assertEquals(
        "dave", password, "On SQL exception, getPassword should return default 'dave'");

    // Verify that error was logged with appropriate message and throwable
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

    org.mockito.Mockito.verify(mockLogger)
        .error(messageCaptor.capture(), throwableCaptor.capture());

    org.junit.jupiter.api.Assertions.assertTrue(
        messageCaptor.getValue().contains("Database error during password retrieval"),
        "Expected database error message in log");

    org.junit.jupiter.api.Assertions.assertTrue(
        throwableCaptor.getValue() instanceof java.sql.SQLException,
        "Logged throwable should be the SQLException that occurred");
  }

  @Test
  @DisplayName("getPassword logs unexpected Exceptions via SLF4J logger")
  void getPassword_logsUnexpectedExceptionsViaSlf4j() throws Exception {
    // Arrange
    LessonDataSource dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    // Make dataSource.getConnection throw a generic exception to hit the outer catch block
    when(dataSource.getConnection()).thenThrow(new RuntimeException("connection failure"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    // Inject mock logger
    org.slf4j.Logger mockLogger = org.mockito.Mockito.mock(org.slf4j.Logger.class);
    java.lang.reflect.Field logField = SqlInjectionLesson6b.class.getDeclaredField("log");
    logField.setAccessible(true);
    logField.set(null, mockLogger);

    // Act
    String password = lesson.getPassword();

    // Assert
    org.junit.jupiter.api.Assertions.assertEquals(
        "dave", password, "On unexpected exception, default password should be returned");

    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

    org.mockito.Mockito.verify(mockLogger)
        .error(messageCaptor.capture(), throwableCaptor.capture());

    org.junit.jupiter.api.Assertions.assertTrue(
        messageCaptor.getValue().contains("Unexpected error during password retrieval"),
        "Expected unexpected error message in log");

    org.junit.jupiter.api.Assertions.assertTrue(
        throwableCaptor.getValue() instanceof RuntimeException,
        "Logged throwable should be the unexpected RuntimeException");
  }
}
