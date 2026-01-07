package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests focused on the changed behavior in SqlInjectionLesson6b:
 * - Replaced printStackTrace() with structured logging via SLF4J.
 * - Ensure that getPassword() handles SQL and general exceptions gracefully
 *   and that completed() behaves correctly when errors occur.
 *
 * Note: We cannot directly assert against logging output without a logging
 * appender; instead we verify the interaction with the data source and
 * that no exceptions escape to the caller and sensible defaults are used.
 */
public class SqlInjectionLesson6bTest {

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
        when(connection.createStatement(
                anyInt(),
                anyInt()))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
    }

    @Test
    void completed_shouldSucceedWhenUserIdMatchesPasswordFromDatabase() throws Exception {
        // Arrange
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dave"); // password from DB
        String suppliedUserId = "dave"; // matches returned password

        // Act
        AttackResult result = lesson.completed(suppliedUserId);

        // Assert
        assertNotNull(result);
        assertEquals(AttackResult.Status.SUCCESS, result.getLessonStatus());

        // Ensure that query was executed as expected
        verify(statement).executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'");
    }

    @Test
    void completed_shouldFailWhenUserIdDoesNotMatchPasswordFromDatabase() throws Exception {
        // Arrange
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secret_from_db");
        String suppliedUserId = "not-matching";

        // Act
        AttackResult result = lesson.completed(suppliedUserId);

        // Assert
        assertNotNull(result);
        assertEquals(AttackResult.Status.FAILED, result.getLessonStatus());
    }

    @Test
    void getPassword_shouldReturnDefaultWhenSqlExceptionOccurs_withoutThrowing() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(anyInt(), anyInt())).thenThrow(new SQLException("DB error"));

        // Act
        String password = lesson.getPassword();

        // Assert
        // When an exception occurs, the method should fall back to the default "dave"
        // and not propagate the exception to callers.
        assertEquals("dave", password, "On SQL exception, getPassword should return the default password");
    }

    @Test
    void getPassword_shouldReturnPasswordFromDatabaseOnSuccess() throws Exception {
        // Arrange
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("from-db");

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("from-db", password);

        // Verify the fixed logging-related behavior indirectly by ensuring
        // that no exceptions are thrown and the query executes as expected.
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement).executeQuery(queryCaptor.capture());
        String usedQuery = queryCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(
                usedQuery.contains("user_name = 'dave'"),
                "Query should still target the static user 'dave'");
    }

    @Test
    void completed_shouldHandleSqlExceptionGracefully_andReturnFailedResult() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new SQLException("connection failed"));

        // Act
        AttackResult result = lesson.completed("any-user");

        // Assert
        // Even when a DB error occurs, the endpoint should not throw, but
        // return a FAILED AttackResult instead.
        assertNotNull(result);
        assertEquals(AttackResult.Status.FAILED, result.getLessonStatus());
    }
}
