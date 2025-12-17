package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.slf4j.Logger;

/**
 * Delta tests for SqlInjectionLesson6b focusing on:
 * - Replacing printStackTrace() with SLF4J logging.
 * - Ensuring getPassword() behavior is preserved (returns "dave" on errors).
 */
public class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson6b;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        lesson6b = new SqlInjectionLesson6b(dataSource);

        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
    }

    @Test
    void getPassword_logsSqlExceptionAndFallsBackToDefaultPassword() throws Exception {
        // Arrange
        SQLException sqlException = new SQLException("db error");
        when(statement.executeQuery(anyString())).thenThrow(sqlException);

        Logger loggerMock = mock(Logger.class);
        ReflectionTestUtils.setField(lesson6b, "log", loggerMock);

        // Act
        String password = lesson6b.getPassword();

        // Assert: default password should be returned when an exception occurs
        assertEquals("dave", password, "On SQL exception, getPassword should fall back to default 'dave'");

        // Verify that the exception was logged via SLF4J instead of printStackTrace()
        verify(loggerMock).error(eq("SQL Exception in getPassword: {}"),
                eq(sqlException.getMessage()), eq(sqlException));
    }

    @Test
    void getPassword_logsGeneralExceptionAndFallsBackToDefaultPassword() throws Exception {
        // Arrange: make dataSource.getConnection() throw a generic exception
        RuntimeException runtimeException = new RuntimeException("connection failed");
        when(dataSource.getConnection()).thenThrow(runtimeException);

        Logger loggerMock = mock(Logger.class);
        ReflectionTestUtils.setField(lesson6b, "log", loggerMock);

        // Act
        String password = lesson6b.getPassword();

        // Assert
        assertEquals("dave", password, "On general exception, getPassword should still return default 'dave'");

        // Verify logging of the general exception
        verify(loggerMock).error(eq("General Exception in getPassword: {}"),
                eq(runtimeException.getMessage()), eq(runtimeException));
    }

    @Test
    void getPassword_returnsDatabasePasswordWhenQuerySucceeds() throws Exception {
        // Arrange
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        // Act
        String password = lesson6b.getPassword();

        // Assert: confirms that the refactor did not break normal behavior
        assertEquals("db-password", password);
    }
}
