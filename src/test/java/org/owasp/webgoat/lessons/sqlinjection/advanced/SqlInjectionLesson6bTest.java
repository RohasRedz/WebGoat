// File: src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
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
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
    }

    @Test
    void getPassword_returnsNonNullValueWhenQuerySucceeds() throws Exception {
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("securePassword");

        String password = lesson.getPassword();

        assertNotNull(password);
        assertEquals("securePassword", password);
    }

    @Test
    void getPassword_handlesSqlExceptionWithLoggingWithoutThrowing() throws Exception {
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("DB error"));

        // Capture logging via Mockito spy on logger class if needed; here we just assert no exception is thrown.
        assertDoesNotThrow(() -> {
            String password = lesson.getPassword();
            assertNotNull(password, "Method should still return a non-null password even on error");
        });
    }
}
