package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

    // Focus on getPassword behavior after logging change: still returns non-empty and no exception
    @Test
    @DisplayName("getPassword should return non-empty password without throwing exceptions")
    void getPassword_returnsNonEmptyString() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.createStatement(anyInt(), anyInt())).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.first()).thenReturn(true);
        when(mockResultSet.getString("password")).thenReturn("secret");

        DataSource mockDataSource = mock(DataSource.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);

        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        when(lessonDataSource.getConnection()).thenReturn(mockConnection);

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(lessonDataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        assertNotNull(password, "Password should not be null");
        assertTrue(password.length() > 0, "Password should not be empty");
    }
}
