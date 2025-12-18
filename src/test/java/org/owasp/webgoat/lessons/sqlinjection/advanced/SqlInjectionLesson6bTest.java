package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword returns default when SQLException occurs and does not throw")
    void getPasswordHandlesSqlExceptionWithoutThrowing() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(anyInt(), anyInt())).thenThrow(new SQLException("DB failure"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act & Assert
        String password = assertDoesNotThrow(lesson::getPassword,
                "getPassword should handle SQL exceptions and not throw");

        assertEquals("dave", password, "Unexpected password when SQL exception occurs");
    }

    @Test
    @DisplayName("getPassword does not expose stack trace and still returns resolved password on success")
    void getPasswordReturnsPasswordOnSuccess() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(anyInt(), anyInt())).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secure-password");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("secure-password", password, "Expected password from DB when query succeeds");
    }
}
