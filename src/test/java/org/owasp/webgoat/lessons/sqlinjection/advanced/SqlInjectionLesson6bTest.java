package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("verifyPassword() returns true when input matches stored password without exposing it")
    void verifyPassword_returnsTrueForMatchingPassword_andDoesNotExposePassword() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dave");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        boolean match = lesson.verifyPassword("dave");

        assertTrue(match);
        verify(resultSet).getString("password");
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("verifyPassword() returns false when input does not match stored password")
    void verifyPassword_returnsFalseForNonMatchingPassword() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dave");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        boolean match = lesson.verifyPassword("wrong");

        assertFalse(match);
    }

    @Test
    @DisplayName("verifyPassword() handles missing result row without exposing stack traces")
    void verifyPassword_handlesMissingRowSecurely() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(false);

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        boolean match = lesson.verifyPassword("any");

        assertFalse(match);
    }
}
