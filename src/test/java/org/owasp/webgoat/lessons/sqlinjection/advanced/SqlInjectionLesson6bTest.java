package org.owasp.webgoat.lessons.sqlinjection.advanced;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Delta tests for SqlInjectionLesson6b focusing on secure logging (no printStackTrace).
 */
class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson6b;

    private Connection connection;
    private Statement statement;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        lesson6b = new SqlInjectionLesson6b(dataSource);

        connection = mock(Connection.class);
        statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
    }

    @Test
    void getPassword_logsErrorInsteadOfPrintingStackTrace_onSqlException() throws Exception {
        SQLException sqlException = new SQLException("db failure");
        when(statement.executeQuery(anyString())).thenThrow(sqlException);

        // Spy on logger to ensure error logging happens (no printStackTrace in code anymore)
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            Logger logger = mock(Logger.class);
            loggerFactoryMock.when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                    .thenReturn(logger);

            String password = lesson6b.getPassword();
            assertNotNull(password);

            verify(logger, atLeastOnce())
                    .error(eq("SQL Exception during password retrieval: {}"), eq(sqlException.getMessage()));
        }
    }
}
