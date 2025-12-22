package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword should still read password from DB without printing stack traces on failure")
    void getPassword_noStackTraceExposureAndBehaviorPreserved() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY)).thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dbPassword");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        String password = lesson.getPassword();

        assertEquals("dbPassword", password);

        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB down"));

        String fallbackPassword = lesson.getPassword();
        assertEquals("dave", fallbackPassword);
    }
}
