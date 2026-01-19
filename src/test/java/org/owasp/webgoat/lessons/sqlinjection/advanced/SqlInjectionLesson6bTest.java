/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword should return value from DB and not fall back to hardcoded default")
    void getPassword_usesDatabaseValue_withoutHardcodedDefault() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dbPasswordValue");

        // Act
        String password = lesson.getPassword();

        // Assert
        // Before the fix, the method initialized password to "dave" and could return that default.
        // Now it must return only the DB-derived value when present.
        assertEquals("dbPasswordValue", password);
    }

    @Test
    @DisplayName("getPassword should not return hardcoded password when DB lookup fails")
    void getPassword_doesNotReturnHardcodedOnFailure() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Simulate an error getting a connection, triggering the catch block
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB unavailable"));

        // Act
        String password = lesson.getPassword();

        // Assert
        // After the fix, password is initialized to empty string instead of "dave".
        // If the DB call fails, it should not fall back to a hardcoded password.
        assertEquals("", password);
    }
}
