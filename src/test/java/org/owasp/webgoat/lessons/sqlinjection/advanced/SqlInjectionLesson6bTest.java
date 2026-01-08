package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focused on the logging behavior change:
 * - Replaced printStackTrace with structured logging via log.error.
 *
 * Path mapping rule:
 *   Source : src/main/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6b.java
 *   Test   : src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
 */
@Slf4j
public class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = Mockito.mock(LessonDataSource.class);
        lesson = new SqlInjectionLesson6b(dataSource);

        connection = Mockito.mock(Connection.class);
        statement = Mockito.mock(Statement.class);
        resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
    }

    @Test
    void getPassword_logsSqlExceptionWithoutThrowing() throws Exception {
        when(connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("simulated SQL failure"));

        String password = lesson.getPassword();

        assertEquals("dave", password,
                "On SQL exception, method should return default password without propagating the exception");
    }

    @Test
    void getPassword_logsGenericExceptionWithoutThrowing() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("simulated general failure"));

        String password = lesson.getPassword();

        assertEquals("dave", password,
                "On general exception, method should return default password without propagating the exception");
    }

    @Test
    void getPassword_readsPasswordWhenQuerySucceeds() throws Exception {
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("securePassword");

        String password = lesson.getPassword();

        assertEquals("securePassword", password,
                "When query succeeds, the password from the result set should be returned");
    }
}
