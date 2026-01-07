package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on the behavior changed by the fix:
 * - Hardcoded default password "dave" removed (password should be null if DB lookup fails).
 * - getPassword() must build the same SQL query but must not fall back to a hardcoded value.
 * - Note: Logging changes from printStackTrace() to log.error() are not asserted here to
 *   keep tests deterministic and focused on functional behavior related to the vulnerability.
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
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(resultSet);
  }

  @Test
  void getPassword_returnsNullWhenNoResultAndDoesNotUseHardcodedDefault() throws Exception {
    // Arrange: simulate that the query returns an empty result set
    when(resultSet != null && resultSet.first()).thenReturn(false);
    when(resultSet.first()).thenReturn(false);

    // Act
    String password = lesson.getPassword();

    // Assert: verify the query is the expected one (unchanged shape)
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(statement).executeQuery(queryCaptor.capture());
    String usedQuery = queryCaptor.getValue();
    assertTrue(
        usedQuery.contains(
            "SELECT password FROM user_system_data WHERE user_name = 'dave'"),
        "Expected the query to select password for user 'dave'");

    // Assert: with the fix, there is no hardcoded default like \"dave\";
    // when there is no DB result, password must be null rather than a static fallback.
    assertNull(
        password,
        "Expected null password when DB returns no result, not a hardcoded default");
  }
}
