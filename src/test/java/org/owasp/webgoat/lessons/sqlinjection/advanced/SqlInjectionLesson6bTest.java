package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        lesson = new SqlInjectionLesson6b(dataSource);

        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
    }

    @Test
    void getPassword_returnsPasswordFromResultSet() throws Exception {
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dave-secret");

        String password = lesson.getPassword();

        assertEquals("dave-secret", password);
    }

    @Test
    void getPassword_handlesSqlExceptionWithoutPropagating() throws Exception {
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> {
            String password = lesson.getPassword();
            // When exception occurs, method currently returns null
            assertEquals(null, password);
        });
    }
}
