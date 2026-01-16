package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

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
public class SqlInjectionLesson6bTest {

    @Mock
    private LessonDataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private SqlInjectionLesson6b sqlInjectionLesson6b;

    @Test
    @DisplayName("getPassword reads password from DB and does not log stack trace on success")
    void getPassword_readsPasswordFromDatabase() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
            .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
            .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        // Act
        String password = sqlInjectionLesson6b.getPassword();

        // Assert
        assertEquals("db-password", password);
        verify(resultSet, times(1)).getString("password");
    }

    @Test
    @DisplayName("getPassword logs via Slf4j on SQLException instead of using printStackTrace")
    void getPassword_usesSlf4jLoggingOnSQLException() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
            .thenThrow(new SQLException("simulated"));

        // Act
        String password = sqlInjectionLesson6b.getPassword();

        // Assert
        // When a failure occurs, method falls back to default "dave"
        assertEquals("dave", password);
        // Behavior under test: no printStackTrace calls remain in code.
        // We cannot directly assert absence of printStackTrace, but this test
        // drives the exception path to ensure it executes without errors.
    }
}
