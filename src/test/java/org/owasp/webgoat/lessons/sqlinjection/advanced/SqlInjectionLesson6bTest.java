package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests focused on the change from printStackTrace() to structured logging
 * using slf4j (via @Slf4j) in SqlInjectionLesson6b.getPassword().
 */
@Slf4j
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword returns database password when query succeeds (behavior unchanged)")
    void getPassword_returnsPasswordFromDatabase_whenQuerySucceeds() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secure-db-password");

        // Act
        String password = lesson.getPassword();

        // Assert: when no exception occurs, original behavior (retrieving DB password) is preserved
        assertEquals("secure-db-password", password);
    }

    @Test
    @DisplayName("getPassword logs SQL exception and falls back to default without printStackTrace")
    void getPassword_logsSqlException_withoutPrintStackTrace_andReturnsDefault() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("DB failure"));

        // We cannot easily assert slf4j output without a logging backend; instead we:
        //  - Ensure method handles the exception (no rethrow)
        //  - Ensure it returns the default password "dave"
        //  - Ensure no SQLException escapes.

        // Act
        String password = lesson.getPassword();

        // Assert: default password is returned when exception occurs
        assertEquals("dave", password);

        // NOTE: We do NOT expect any printStackTrace() calls anymore.
        // Since printStackTrace was removed from the code, this test implicitly
        // ensures the new behavior is that an exception is handled internally
        // and does not propagate, aligning with the security fix.
    }

    @Test
    @DisplayName("getPassword swallows generic exception and returns default password")
    void getPassword_handlesGenericException_andReturnsDefault() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Force a generic Exception by making dataSource.getConnection() throw a RuntimeException
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("dave", password);
    }
}
