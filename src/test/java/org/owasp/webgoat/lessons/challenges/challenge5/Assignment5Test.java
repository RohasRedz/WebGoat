/*
 * SPDX-FileCopyrightText: Copyright © 2017 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the changed SQL preparation logic.
 *
 * Original behavior: SQL query was built via string concatenation with user input.
 * Updated behavior: Uses parameterized PreparedStatement with placeholders and setString.
 *
 * These tests verify:
 * - The code uses parameter binding (prepared-statement style invocation) and
 *   not string concatenation for user input.
 * - The functional behavior (success on matching credentials, failure otherwise)
 *   remains intact when the PreparedStatement is executed.
 */
public class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized PreparedStatement and succeeds on correct credentials")
  void login_usesParameterizedQuery_andReturnsSuccessOnValidCredentials() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    DataSource ds = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult result = assignment5.login("Larry", "secret");

    // Assert
    // If the implementation regresses to string concatenation, this test would either:
    // - call a different SQL string than expected, causing this stub not to match, or
    // - result in unexpected SQL or failures.
    assertEquals("success", result.getLessonStatus().toString().toLowerCase());
    // we also implicitly verify that the query string with '?' is used by our stubbed expectation
  }

  @Test
  @DisplayName("login fails when username is not Larry, independent of SQL parameterization")
  void login_rejectsNonLarryUser_beforeSqlExecution() throws Exception {
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    AttackResult result = assignment5.login("Bob", "whatever");

    assertEquals("failed", result.getLessonStatus().toString().toLowerCase());
  }

  @Test
  @DisplayName("login fails when username or password is empty (input validation preserved)")
  void login_failsOnEmptyInputs() throws Exception {
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    AttackResult emptyUsername = assignment5.login("", "pw");
    AttackResult emptyPassword = assignment5.login("Larry", "");

    assertEquals("failed", emptyUsername.getLessonStatus().toString().toLowerCase());
    assertEquals("failed", emptyPassword.getLessonStatus().toString().toLowerCase());
  }
}
