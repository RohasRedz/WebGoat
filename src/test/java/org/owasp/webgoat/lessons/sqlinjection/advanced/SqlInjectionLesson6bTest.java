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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.webgoat.container.LessonDataSource;

@ExtendWith(MockitoExtension.class)
class SqlInjectionLesson6bTest {

    @Mock
    private LessonDataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private SqlInjectionLesson6b lesson6b;

    @Test
    @DisplayName("getPassword() returns password from DB when query succeeds")
    void getPassword_returnsPasswordFromDatabase() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        // Act
        String password = lesson6b.getPassword();

        // Assert
        assertEquals("db-password", password);
    }

    @Test
    @DisplayName("getPassword() logs exception and returns default when SQL error occurs without printing stack trace")
    void getPassword_logsSqlExceptionAndReturnsDefault() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("DB error"));

        // Act
        String password = lesson6b.getPassword();

        // Assert:
        // - Behavior: default password \"dave\" is returned when exception occurs
        // - Security: we can't assert on internal logging here, but this test ensures that
        //   the method swallows the exception and does not propagate stack traces.
        assertEquals("dave", password);
    }
}
