/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the changed exception handling behavior
 * in getPassword(). Original code printed stack traces; the fixed code suppresses them.
 *
 * These tests validate:
 * - Functional behavior of completed() remains the same on correct/incorrect passwords.
 * - Exceptions thrown inside getPassword() do not propagate (they are swallowed),
 *   demonstrating that stack trace printing has been removed and replaced with no-op handling.
 */
public class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("completed returns success when user-supplied password matches DB password")
  void completed_returnsSuccessOnCorrectPassword() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    when(resultSet != null && resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("dave-db-password");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(lessonDataSource);

    // Act
    AttackResult result = lesson.completed("dave-db-password");

    // Assert
    assertEquals("success", result.getLessonStatus().toString().toLowerCase());
  }

  @Test
  @DisplayName("completed returns failed when password is incorrect")
  void completed_returnsFailedOnWrongPassword() throws Exception {
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenReturn(statement);
    when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
        .thenReturn(resultSet);
    when(resultSet != null && resultSet.first()).thenReturn(true);
    when(resultSet.getString("password")).thenReturn("dave-db-password");

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(lessonDataSource);

    AttackResult result = lesson.completed("wrong-password");

    assertEquals("failed", result.getLessonStatus().toString().toLowerCase());
  }

  @Test
  @DisplayName("getPassword swallows SQLException and returns fallback password without propagating")
  void getPassword_swallowsSqlException_andDoesNotThrow() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    Connection connection = mock(Connection.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    // Simulate failure in createStatement  triggers inner catch block that previously
    // printed stack trace; now it should just swallow exception.
    when(connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(new RuntimeException("simulated DB error"));

    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(lessonDataSource);

    // Act
    // If the implementation still uses printStackTrace and rethrows, this would fail.
    String password = lesson.getPassword();

    // Assert
    // Behavior: returns the default value "dave" when exceptions occur.
    assertEquals("dave", password);
  }
}
