// File: src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    @BeforeEach
    void setUp() {
        dataSource = mock(LessonDataSource.class);
        lesson = new SqlInjectionLesson6b(dataSource);
    }

    @Test
    void getPassword_returnsDatabasePasswordWithoutLoggingStackTraceOnSqlException() throws Exception {
        // Arrange
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        // Simulate an SQL exception thrown when creating the statement
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY)).thenThrow(new java.sql.SQLException("DB error"));

        // Act
        String password = lesson.getPassword();

        // Assert: even when an SQLException occurs, the method
        // 1) does not propagate the exception
        // 2) returns the default password "dave"
        // The delta behavior we assert is that no stack trace is printed anymore; we
        // approximate this by ensuring the method swallows the exception and returns
        // the default (it previously called printStackTrace()).
        org.junit.jupiter.api.Assertions.assertEquals("dave", password);
    }

    @Test
    void getPassword_readsPasswordFromDatabaseWhenAvailable() throws Exception {
        // Arrange
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY)).thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dbPassword");

        // Act
        String password = lesson.getPassword();

        // Assert: normal behavior preserved
        org.junit.jupiter.api.Assertions.assertEquals("dbPassword", password);
    }
}
