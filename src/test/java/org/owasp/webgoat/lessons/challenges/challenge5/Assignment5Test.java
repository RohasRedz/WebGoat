/*
 * SPDX-FileCopyrightText: Copyright © 2017 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on replacement of string-concatenated SQL with a
 * parameterized PreparedStatement to prevent SQL injection.
 */
public class Assignment5Test {

  @Test
  void login_shouldUseParameterizedQueryWithUserInputs() throws Exception {
    LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
    Flags flags = Mockito.mock(Flags.class);
    Assignment5 assignment = new Assignment5(dataSource, flags);

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(true);
    Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

    String username = "Larry";
    String password = "password' OR '1'='1";

    AttackResult result = assignment.login(username, password);

    // Verify that we are using a parameterized query, not string concatenation
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String usedSql = sqlCaptor.getValue();
    // Ensure the prepared SQL uses placeholders instead of concatenated user data
    // (no direct injection of user input into the SQL literal)
    org.junit.jupiter.api.Assertions.assertTrue(
        usedSql.contains("userid = ?") && usedSql.contains("password = ?"));

    // Verify parameters are bound correctly and in order
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // Sanity: original behavior preserved (successful login case)
    assertEquals("challenge.solved", result.getLessonKey());
  }
}
