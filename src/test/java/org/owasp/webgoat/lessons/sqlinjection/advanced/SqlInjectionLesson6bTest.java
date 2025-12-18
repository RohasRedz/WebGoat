package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword returns value from database (behavior preserved)")
    void getPassword_returnsValueFromDatabase() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
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

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        String password = lesson.getPassword();

        assertEquals("db-password", password,
                "Expected getPassword to return the DB password when query succeeds");
    }

    @Test
    @DisplayName("getPassword handles SQL exception gracefully and logs error without throwing")
    void getPassword_handlesSqlExceptionAndLogsError() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
            .thenThrow(new RuntimeException("SQL problem"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        String password = lesson.getPassword();

        assertEquals("dave", password,
                "Expected default password to be returned when an exception occurs");
    }
}
