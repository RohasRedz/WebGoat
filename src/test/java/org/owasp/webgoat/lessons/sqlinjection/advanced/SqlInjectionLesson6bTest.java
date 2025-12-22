// File path: src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on changed behavior:
 * - No hard-coded default password is used when DB returns no value.
 * - Exceptions are logged via log.error instead of printStackTrace and do not escape.
 */
class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    private SqlInjectionLesson6b lesson;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);

        lesson = new SqlInjectionLesson6b(dataSource);
    }

    @Test
    @DisplayName("getPassword should return DB value and not a hard-coded default")
    void getPasswordReturnsDbValue_notHardCoded() throws Exception {
        // Arrange
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("db-password", password);
    }

    @Test
    @DisplayName("getPassword should return null when DB has no rows instead of 'dave'")
    void getPasswordReturnsNullWhenNoResult() throws Exception {
        // Arrange
        when(resultSet.first()).thenReturn(false);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertNull(password, "When no DB value is found, password should be null, not a hard-coded default");
    }

    @Test
    @DisplayName("getPassword should handle SQLExceptions gracefully and not throw to caller")
    void getPasswordHandlesSqlExceptionGracefully() throws Exception {
        // Arrange
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("DB error"));

        // Act & Assert
        assertDoesNotThrow(() -> {
            String pwd = lesson.getPassword();
            assertNull(pwd, "On SQL error, password should remain null");
        });
    }

    @Test
    @DisplayName("getPassword should handle connection exceptions gracefully and not throw to caller")
    void getPasswordHandlesConnectionExceptionGracefully() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // Act & Assert
        assertDoesNotThrow(() -> {
            String pwd = lesson.getPassword();
            assertNull(pwd, "On connection error, password should remain null");
        });
    }

    @Test
    @DisplayName("getPassword must not leak hard-coded password in SQL query")
    void getPasswordQueryDoesNotUseHardCodedPassword() throws Exception {
        // Arrange
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        when(resultSet.first()).thenReturn(false);

        // Act
        lesson.getPassword();

        // Assert
        verify(statement).executeQuery(queryCaptor.capture());
        String sqlUsed = queryCaptor.getValue();

        // The SQL may still use a fixed user_name, but not a hard-coded password default.
        assertTrue(sqlUsed.contains("user_name = 'dave'"));
        assertFalse(sqlUsed.toLowerCase().contains("password = 'dave'"),
                "SQL must not embed 'dave' as a hard-coded password");
    }
}
