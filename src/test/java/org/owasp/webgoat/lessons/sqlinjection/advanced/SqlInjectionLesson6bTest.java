package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

public class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword should no longer fall back to hardcoded default value")
  void getPassword_noHardcodedDefaultPassword() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(lessonDataSource.getConnection()).thenReturn(connection);
    Mockito.when(
            connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    Mockito.when(
            statement.executeQuery(
                "SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    // Simulate no rows so that the method would previously return the hardcoded "dave"
    Mockito.when(resultSet.first()).thenReturn(false);

    SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(lessonDataSource);

    // Act
    String password = endpoint.getPassword();

    // Assert
    assertTrue(
        password == null || !"dave".equals(password),
        "getPassword must not return the hardcoded default 'dave' when no DB row is found");
  }

  @Test
  @DisplayName("getPassword should return DB value when available")
  void getPassword_returnsDatabaseValue() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    Connection connection = Mockito.mock(Connection.class);
    Statement statement = Mockito.mock(Statement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(lessonDataSource.getConnection()).thenReturn(connection);
    Mockito.when(
            connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    Mockito.when(
            statement.executeQuery(
                "SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    Mockito.when(resultSet.first()).thenReturn(true);
    Mockito.when(resultSet.getString("password")).thenReturn("from-db");

    SqlInjectionLesson6b endpoint = new SqlInjectionLesson6b(lessonDataSource);

    // Act
    String password = endpoint.getPassword();

    // Assert
    assertEquals("from-db", password, "Expected password to come from DB row, not a default");
  }

  @Test
  @DisplayName("completed should still succeed when provided password matches returned value")
  void completed_usesPasswordFromGetPassword() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = Mockito.mock(LessonDataSource.class);
    SqlInjectionLesson6b endpoint =
        Mockito.spy(new SqlInjectionLesson6b(lessonDataSource));

    Mockito.doReturn("expected-secret").when(endpoint).getPassword();

    // Act
    AttackResult successResult = endpoint.completed("expected-secret");
    AttackResult failResult = endpoint.completed("wrong");

    // Assert
    assertTrue(
        successResult.getLessonCompleted(),
        "Lesson should be completed when supplied password matches getPassword()");
    assertTrue(
        !failResult.getLessonCompleted(),
        "Lesson must not be completed when supplied password does not match getPassword()");
  }
}
