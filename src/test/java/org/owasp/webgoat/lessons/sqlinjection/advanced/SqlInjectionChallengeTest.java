package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Delta tests for SqlInjectionChallenge focusing on the SQL injection fix for the user lookup:
 * - Before fix: SELECT query was built via string concatenation with username.
 * - After fix:  SELECT query uses PreparedStatement with parameter binding.
 *
 * We verify:
 * - The SELECT query string uses a single parameter placeholder ("?") and not an inline username.
 * - The PreparedStatement receives the username via setString.
 */
public class SqlInjectionChallengeTest {

  private LessonDataSource dataSource;
  private SqlInjectionChallenge challenge;

  @BeforeEach
  void setUp() {
    dataSource = Mockito.mock(LessonDataSource.class);
    challenge = new SqlInjectionChallenge(dataSource);
  }

  @Test
  void registerNewUser_shouldUseParameterizedSelectForUserLookup() throws Exception {
    // Arrange
    String username = "victim";
    String email = "victim@example.com";
    String password = "secret";

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement selectStatement = Mockito.mock(PreparedStatement.class);
    PreparedStatement insertStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);

    // Capture SQL for SELECT query
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    when(connection.prepareStatement(sqlCaptor.capture()))
        .thenReturn(selectStatement)
        .thenReturn(insertStatement); // first call: select, second: insert

    when(selectStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false); // user does not exist, go to insert

    // Act
    AttackResult result = challenge.registerNewUser(username, email, password);

    // Assert - verify SELECT SQL string uses placeholder
    String selectSql = sqlCaptor.getAllValues().get(0);
    assertEquals(
        "select userid from sql_challenge_users where userid = ?",
        selectSql,
        "User lookup query must be parameterized and not contain raw username");

    // Assert - username is bound via setString on the select statement
    verify(selectStatement).setString(1, username);
  }
}
