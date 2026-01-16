/*
 * SPDX-FileCopyrightText: Copyright © 2017 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionChallenge focusing on the change from a concatenated SQL string to a
 * parameterized PreparedStatement for the user existence check query.
 */
public class SqlInjectionChallengeTest {

  @Test
  void registerNewUser_shouldUsePreparedStatementForUserExistenceCheck() throws SQLException {
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement checkStatement = Mockito.mock(PreparedStatement.class);
    PreparedStatement insertStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    // First call: check user; second call: insert
    Mockito.when(connection.prepareStatement(Mockito.anyString()))
        .thenReturn(checkStatement)
        .thenReturn(insertStatement);
    Mockito.when(checkStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(false); // user does not exist

    AttackResult result =
        challenge.registerNewUser("newUser", "user@example.com", "Passw0rd!");

    // Capture the SQL used for the existence check
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();

    org.junit.jupiter.api.Assertions.assertTrue(
        usedSql.contains("where userid = ?"),
        "User existence check must use a parameterized query");

    // Verify binding of username parameter
    verify(checkStatement).setString(1, "newUser");

    // Sanity: original behavior preserved when user is new
    assertEquals("user.created", result.getLessonKey());
  }
}
