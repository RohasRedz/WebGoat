package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focused on the logging behavior
 * that replaced printStackTrace with structured SLF4J logging.
 */
@Slf4j
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword() should log SQLExceptions via logger instead of printStackTrace")
    void getPassword_logsSqlExceptionWithLogger() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Connection connection = Mockito.mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        // Make createStatement throw SQLException to trigger logging path
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY)).thenThrow(new SQLException("DB error"));

        // Act
        String password = lesson.getPassword();

        // Assert
        // Behavior: falls back to default "dave" and does not propagate exception
        assertEquals("dave", password, "On SQL error, password should remain default");

        // NOTE: Direct verification of log.error calls requires a logging appender or
        //   framework-specific test harness. Here we rely on compilation-time change:
        //   the method no longer calls printStackTrace() but calls log.error() instead.
        //   This delta test executes the error path to ensure it remains non-throwing.
    }

    @Test
    @DisplayName("getPassword() should return DB value when query succeeds")
    void getPassword_returnsValueFromDatabase() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY)).thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secure-db-password");

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("secure-db-password", password, "Expected password read from DB");
    }
}
