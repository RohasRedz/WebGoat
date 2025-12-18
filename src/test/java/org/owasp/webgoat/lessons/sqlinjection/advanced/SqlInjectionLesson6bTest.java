package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword logs SQL exception via log.error without throwing")
    void getPassword_logsSqlExceptionWithLogger() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("boom"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Spy on the logger via SLF4J (using reflection to inject a mock logger)
        Logger mockLogger = mock(Logger.class);
        injectLogger(lesson, mockLogger);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("dave", password, "On SQL error, password should fall back to default 'dave'");
        verify(mockLogger)
                .error(
                        eq("SQL Exception during password retrieval"),
                        any(SQLException.class));
    }

    @Test
    @DisplayName("getPassword logs general exception via log.error without throwing")
    void getPassword_logsGeneralExceptionWithLogger() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("connection failed"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Logger mockLogger = mock(Logger.class);
        injectLogger(lesson, mockLogger);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("dave", password, "On general error, password should fall back to default 'dave'");
        verify(mockLogger)
                .error(eq("General Exception during password retrieval"), any(RuntimeException.class));
    }

    /**
     * Inject a mock SLF4J logger into the @Slf4j-generated 'log' field.
     * This uses reflection because Lombok generates a private static final logger.
     */
    private void injectLogger(SqlInjectionLesson6b target, Logger logger) throws Exception {
        // TODO: This relies on Lombok's standard 'log' field name; adjust if class changes.
        var field = SqlInjectionLesson6b.class.getDeclaredField("log");
        field.setAccessible(true);
        field.set(null, logger);
    }
}
