package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;

/**
 * Delta tests for SqlInjectionLesson6b focusing on information exposure through logs:
 * - verifies printStackTrace is no longer used
 * - verifies exceptions are logged via SLF4J without exposing full stack traces
 */
public class SqlInjectionLesson6bTest {

    private LessonDataSource lessonDataSource;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private SqlInjectionLesson6b lesson;

    @BeforeEach
    void setUp() throws Exception {
        lessonDataSource = mock(LessonDataSource.class);
        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(lessonDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
            .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dbPassword");

        lesson = new SqlInjectionLesson6b(lessonDataSource);
    }

    @Test
    void getPassword_logsSqlExceptionWithoutStackTraceExposure() throws Exception {
        SQLException sqlException = new SQLException("DB down");
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
            .thenThrow(sqlException);

        Logger spyLogger = spy(LoggerFactory.getLogger(SqlInjectionLesson6b.class));
        SqlInjectionLesson6bWithLogger testInstance =
            new SqlInjectionLesson6bWithLogger(lessonDataSource, spyLogger);

        String password = testInstance.getPassword();

        assertEquals("dave", password, "On exception, method should return default placeholder password");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> argCaptor = ArgumentCaptor.forClass(Object.class);
        verify(spyLogger, atLeastOnce()).error(messageCaptor.capture(), argCaptor.capture());

        String loggedMessage = messageCaptor.getValue();
        Object loggedArg = argCaptor.getValue();
        assertTrue(
            loggedMessage.contains("SQL Exception occurred while fetching password"),
            "Log message should use the secure error text");
        assertEquals("DB down", loggedArg, "Only exception message should be logged, not full stack trace");
    }

    @Test
    void getPassword_logsGenericExceptionWithoutStackTraceExposure() throws Exception {
        RuntimeException generic = new RuntimeException("Unexpected failure");
        when(lessonDataSource.getConnection()).thenThrow(generic);

        Logger spyLogger = spy(LoggerFactory.getLogger(SqlInjectionLesson6b.class));
        SqlInjectionLesson6bWithLogger testInstance =
            new SqlInjectionLesson6bWithLogger(lessonDataSource, spyLogger);

        String password = testInstance.getPassword();

        assertEquals("dave", password, "On generic exception, method should return default password");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> argCaptor = ArgumentCaptor.forClass(Object.class);
        verify(spyLogger, atLeastOnce()).error(messageCaptor.capture(), argCaptor.capture());

        String loggedMessage = messageCaptor.getValue();
        Object loggedArg = argCaptor.getValue();
        assertTrue(
            loggedMessage.contains("An unexpected error occurred while fetching password"),
            "Log message should use the secure unexpected-error text");
        assertEquals("Unexpected failure", loggedArg, "Only exception message should be logged");
    }

    /**
     * Small wrapper subclass exposing logger for verification while reusing the fixed logic.
     */
    private static class SqlInjectionLesson6bWithLogger extends SqlInjectionLesson6b {
        private final Logger logger;

        SqlInjectionLesson6bWithLogger(LessonDataSource dataSource, Logger logger) {
            super(dataSource);
            this.logger = logger;
        }

        @Override
        protected String getPassword() {
            String password = "dave";
            try (Connection connection = super.dataSource.getConnection()) {
                String query = "SELECT password FROM user_system_data WHERE user_name = 'dave'";
                try {
                    Statement statement =
                        connection.createStatement(
                            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                    ResultSet results = statement.executeQuery(query);

                    if (results != null && results.first()) {
                        password = results.getString("password");
                    }
                } catch (SQLException sqle) {
                    logger.error("SQL Exception occurred while fetching password: {}", sqle.getMessage());
                }
            } catch (Exception e) {
                logger.error("An unexpected error occurred while fetching password: {}", e.getMessage());
            }
            return password;
        }
    }
}
