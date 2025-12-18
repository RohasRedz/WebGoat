package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword returns DB password for user dave when available")
    void getPasswordReturnsDatabasePassword() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("dbPasswordForDave");

        String password = lesson.getPassword();

        assertEquals("dbPasswordForDave", password);

        verify(statement).executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'");
        InOrder inOrder = inOrder(connection, statement, resultSet);
        inOrder.verify(connection).createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        inOrder.verify(statement).executeQuery(anyString());
    }
}
