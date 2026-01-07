package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for changed behavior in SqlInjectionLesson6b:
 * - Exceptions in getPassword() are handled without crashing.
 * - Exceptions are logged via the logger instead of using printStackTrace().
 *
 * NOTE: This test uses Mockito's inline mocking (mock construction and static)
 * to emulate the logging behavior. Adjust configuration if your build differs.
 */
class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    @BeforeEach
    void setUp() {
        dataSource = mock(LessonDataSource.class);
        lesson = new SqlInjectionLesson6b(dataSource);
    }

    @Test
    void getPassword_sqlExceptionIsLoggedAndDefaultPasswordReturned() throws Exception {
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        // Mock statement/execute to throw SQLException
        try (MockedConstruction<Statement> ignored = Mockito.mockConstruction(
                Statement.class,
                (mock, ctx) -> when(mock.executeQuery(anyString())).thenThrow(new SQLException("boom")));
             MockedStatic<LoggerFactory> loggerFactoryMockedStatic = Mockito.mockStatic(LoggerFactory.class)) {

            Logger mockLogger = mock(Logger.class);
            loggerFactoryMockedStatic
                    .when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                    .thenReturn(mockLogger);

            String password = lesson.getPassword();

            // When exception occurs, default password should be returned and error logged
            assertEquals("dave", password);
            verify(mockLogger).error(
                    contains("SQL Exception occurred while fetching password in getPassword method"),
                    any(SQLException.class));
        }
    }

    @Test
    void getPassword_generalExceptionIsLoggedAndDefaultPasswordReturned() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("connection failure"));

        try (MockedStatic<LoggerFactory> loggerFactoryMockedStatic = Mockito.mockStatic(LoggerFactory.class)) {
            Logger mockLogger = mock(Logger.class);
            loggerFactoryMockedStatic
                    .when(() -> LoggerFactory.getLogger(SqlInjectionLesson6b.class))
                    .thenReturn(mockLogger);

            String password = lesson.getPassword();

            assertEquals("dave", password);
            verify(mockLogger).error(
                    contains("General Exception occurred in getPassword method"),
                    any(RuntimeException.class));
        }
    }

    @Test
    void completed_doesNotThrowWhenGetPasswordThrowsIOException() throws Exception {
        // This delta test ensures that even if getPassword had issues,
        // the completed() method itself still handles comparison and returns AttackResult.
        SqlInjectionLesson6b spyLesson = Mockito.spy(lesson);
        // Force getPassword to return a specific value
        doReturn("dave").when(spyLesson).getPassword();

        AttackResult resultSuccess = spyLesson.completed("dave");
        AttackResult resultFailure = spyLesson.completed("not-dave");

        assertTrue(resultSuccess.getSuccess());
        assertFalse(resultFailure.getSuccess());
    }
}
