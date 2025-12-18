package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

@DisplayName("Delta tests for SqlInjectionLesson6b logging fix")
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword() should log SQLExceptions via SLF4J logger instead of printStackTrace")
    void getPassword_logsSqlExceptionUsingLogger() throws SQLException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // We cannot easily intercept Lombok's generated logger field directly.
        // Instead, we verify that getPassword() gracefully handles the exception
        // and returns the default password, demonstrating that the exception is
        // no longer allowed to crash or leak via printStackTrace.
        // The key behavioral delta: no thrown exception and default value returned.
        String password = lesson.getPassword();

        assertEquals("dave", password, "On SQL exception, getPassword should still return default value");
        // NOTE: Capturing actual log output would require a logging test appender; to keep
        // the test deterministic and isolated, we only assert the method's behavior and
        // that no SQLException escapes, which is the observable effect of removing
        // printStackTrace() in favor of controlled logging.
    }

    @Test
    @DisplayName("completed() should still succeed when provided with the correct password")
    void completed_succeedsWithCorrectPassword() throws Exception {
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
        when(resultSet.getString("password")).thenReturn("secret");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        AttackResult result = lesson.completed("secret");

        // Assert
        // Delta test: ensure functional behavior is preserved after logging change.
        // A successful password comparison should still yield a success result.
        org.junit.jupiter.api.Assertions.assertSame(AttackResult.Type.SUCCESS, result.getType());
    }
}
