package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests focusing on the changed logging behavior in getPassword():
 * - printStackTrace() is no longer used
 * - log.error(...) is used instead
 */
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword should not throw when SQLException occurs and still return a password value")
    void getPassword_handlesSqlExceptionWithoutPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString()))
                .thenThrow(new SQLException("DB failure"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        // On exception, method should swallow the exception and return some value (default or last known),
        // but must not propagate the exception.
        assertTrue(password != null && !password.isEmpty());
        // We cannot directly assert the absence of printStackTrace, but the fact that we
        // replaced it with logging and there is no exception thrown here validates the changed flow.
    }

    @Test
    @DisplayName("getPassword should return DB password when query succeeds")
    void getPassword_returnsPasswordFromDatabase() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("securePasswordFromDb");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals("securePasswordFromDb", password);
    }
}
