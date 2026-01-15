package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionChallenge focusing on the change from a concatenated
 * Statement to a parameterized PreparedStatement, closing SQL injection vectors.
 */
public class SqlInjectionChallengeTest {

  @Test
  void registerNewUser_usesPreparedStatementWithParameterBinding() throws SQLException {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    Connection connection = mock(Connection.class);
    PreparedStatement checkStmt = mock(PreparedStatement.class);
    PreparedStatement insertStmt = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
        .thenReturn(checkStmt);
    when(checkStmt.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);
    when(connection.prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)")).thenReturn(insertStmt);

    AttackResult result =
        challenge.registerNewUser("user' OR '1'='1", "email@example.com", "password");

    verify(checkStmt).setString(1, "user' OR '1'='1");
    verify(checkStmt).executeQuery();
    verify(insertStmt).setString(1, "user' OR '1'='1");
    verify(insertStmt).setString(2, "email@example.com");
    verify(insertStmt).setString(3, "password");
    verify(insertStmt).execute();

    assertFalse(result.getLessonCompleted(), "Registration should not directly complete the lesson");

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();
    assertFalse(usedSql.contains("user' OR '1'='1"));
  }

  @Test
  void registerNewUser_failsForEmptyInput() {
    LessonDataSource dataSource = mock(LessonDataSource.class);
    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    AttackResult result = challenge.registerNewUser("", "a@b.c", "pwd");

    assertFalse(result.getLessonCompleted(), "Empty username should fail validation");
  }
}
