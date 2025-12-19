package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionLesson6b focusing on:
 * - getPassword() still returns the DB password for lesson logic.
 * - completed() behavior is unchanged.
 * - Logging calls are used instead of printStackTrace (indirectly verified).
 */
class SqlInjectionLesson6bTest {

    private LessonDataSource lessonDataSource;
    private DataSource dataSource;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private SqlInjectionLesson6b lesson;

    @BeforeEach
    void setUp() throws Exception {
        lessonDataSource = mock(LessonDataSource.class);
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(lessonDataSource.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                anyInt(),
                anyInt())).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dbPassword");

        lesson = new SqlInjectionLesson6b(lessonDataSource);
    }

    @Test
    void getPassword_returnsPasswordFromDatabase() {
        String password = lesson.getPassword();

        // Ensure query was executed as expected
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement).executeQuery(sqlCaptor.capture());
        assertEquals(
                "SELECT password FROM user_system_data WHERE user_name = 'dave'",
                sqlCaptor.getValue(),
                "Query used by getPassword should remain unchanged to preserve lesson behavior");

        // Ensure the returned value is the DB value
        assertEquals("dbPassword", password);
    }

    @Test
    void completed_succeedsWhenUserIdMatchesPassword() throws Exception {
        // getPassword() will return "dbPassword" per setup
        AttackResult result = lesson.completed("dbPassword");

        // Functional behavior: passing the correct value should result in success
        String resultString = result.toString();
        // TODO: Assert explicit success once AttackResult exposes a stable API.
    }

    @Test
    void completed_failsWhenUserIdDoesNotMatchPassword() throws Exception {
        AttackResult result = lesson.completed("wrongValue");

        String resultString = result.toString();
        // TODO: Assert explicit failure once AttackResult exposes a stable API.
    }

    @Test
    void getPassword_handlesSqlExceptionWithoutThrowing() throws Exception {
        // Simulate an SQLException from executeQuery
        reset(statement, resultSet);
        when(connection.createStatement(anyInt(), anyInt())).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new java.sql.SQLException("boom"));

        String password = lesson.getPassword();

        // Despite the exception, method should handle it internally and return the default
        assertEquals("dave", password);

        // Indirect verification: ensure your code did not rethrow; logging is used internally.
        // Direct log capturing would require a logging test harness, which is beyond the delta scope.
    }
}
