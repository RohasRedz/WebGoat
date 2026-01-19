// File: src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("completed uses password retrieved from DB and still succeeds on correct value")
    void completed_usesDatabasePasswordAndSucceedsOnMatch() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secret-from-db");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        AttackResult result = lesson.completed("secret-from-db");

        // Assert
        assertEquals(true, result.getLessonCompleted(),
                "Expected success when supplied userid_6b matches password retrieved from DB");

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement).executeQuery(queryCaptor.capture());
        String query = queryCaptor.getValue();
        // Ensure the query is as expected; we only validate that the fix did not alter its semantics
        assertEquals("SELECT password FROM user_system_data WHERE user_name = 'dave'", query);
    }

    @Test
    @DisplayName("getPassword returns default when SQLException occurs and does not propagate exception")
    void getPassword_handlesSQLException_andReturnsDefault() throws SQLException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("DB failure"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertEquals("dave", password,
                "When DB access fails, getPassword should return the default value instead of leaking/throwing");

        // Note: We do not assert on logging output; this test ensures control flow and non-exposure via exceptions.
    }
}
