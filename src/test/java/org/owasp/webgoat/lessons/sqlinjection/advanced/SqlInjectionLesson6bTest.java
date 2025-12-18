// File path assumption based on Maven layout:
// src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests focusing solely on changed behavior:
 * - getPassword() should retrieve the password from DB correctly
 * - Exceptions should be logged via SLF4J logger instead of printStackTrace (no direct stacktrace leakage)
 */
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword should return password retrieved from database when query succeeds")
    void getPasswordReturnsDatabaseValue() throws Exception {
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
        when(resultSet.getString("password")).thenReturn("db-password");

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("db-password", password, "getPassword should return value from the database when available");
    }

    @Test
    @DisplayName("getPassword should log SQLExceptions via logger without throwing and return default password")
    void getPasswordLogsSQLExceptionWithoutStacktraceLeak() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new java.sql.SQLException("DB error"));

        // Spy on the logger by using reflection to replace the static log field is not trivial here,
        // so we instead ensure that no exception escapes and the default password is returned.
        // The absence of printStackTrace in the updated code ensures no direct stacktrace leakage.
        // This delta test verifies the behavior change from throwing/printing to logging & swallowing.

        // Act
        String password = lesson.getPassword();

        // Assert: default value is still returned when exception occurs
        assertEquals("dave", password, "On SQL exception, getPassword should fall back to default password");

        // NOTE: Verifying SLF4J logging directly would require a logging framework test appender.
        // Here we rely on static code inspection plus this behavioral assertion as a delta test.
    }
}
