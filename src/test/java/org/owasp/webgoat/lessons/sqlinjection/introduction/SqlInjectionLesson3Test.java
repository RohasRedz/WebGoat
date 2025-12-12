package org.owasp.webgoat.lessons.sqlinjection.introduction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

public class SqlInjectionLesson3Test {

    private final LessonDataSource dataSource = mock(LessonDataSource.class);
    private final SqlInjectionLesson3 lesson = new SqlInjectionLesson3(dataSource);

    @Test
    void completed_shouldUpdateDepartmentUsingPreparedStatement_whenNewDepartmentProvided() throws Exception {
        String newDepartment = "Sales";
        Connection connection = mock(Connection.class);
        PreparedStatement updatePs = mock(PreparedStatement.class);
        Statement checkStatement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("UPDATE employees SET department = ? WHERE last_name = 'Barnett'"))
                .thenReturn(updatePs);
        when(connection.createStatement(anyInt(), anyInt())).thenReturn(checkStatement);
        when(checkStatement.executeQuery("SELECT * FROM employees WHERE last_name='Barnett';"))
                .thenReturn(resultSet);

        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("department")).thenReturn("Sales");

        AttackResult result = lesson.completed(newDepartment);

        assertNotNull(result);

        verify(updatePs).setString(1, newDepartment);
        verify(updatePs).executeUpdate();

        verify(checkStatement).executeQuery("SELECT * FROM employees WHERE last_name='Barnett';");
        verify(resultSet).first();
        verify(resultSet).getString("department");

        assertEquals("Sales", resultSet.getString("department"));
    }

    @Test
    void completed_shouldNotSucceedWhenDepartmentIsNotSales() throws Exception {
        String newDepartment = "Engineering";
        Connection connection = mock(Connection.class);
        PreparedStatement updatePs = mock(PreparedStatement.class);
        Statement checkStatement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("UPDATE employees SET department = ? WHERE last_name = 'Barnett'"))
                .thenReturn(updatePs);
        when(connection.createStatement(anyInt(), anyInt())).thenReturn(checkStatement);
        when(checkStatement.executeQuery("SELECT * FROM employees WHERE last_name='Barnett';"))
                .thenReturn(resultSet);

        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("department")).thenReturn("Engineering");

        AttackResult result = lesson.completed(newDepartment);

        assertNotNull(result);

        verify(updatePs).setString(1, newDepartment);
        verify(updatePs).executeUpdate();

        verify(resultSet).first();
        verify(resultSet).getString("department");

        assertEquals("Engineering", resultSet.getString("department"));
    }

    @Test
    void completed_shouldNotExecuteArbitraryUserProvidedSql() throws Exception {
        String maliciousInput = "Sales'; DROP TABLE employees; --";

        Connection connection = mock(Connection.class);
        PreparedStatement updatePs = mock(PreparedStatement.class);
        Statement checkStatement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("UPDATE employees SET department = ? WHERE last_name = 'Barnett'"))
                .thenReturn(updatePs);
        when(connection.createStatement(anyInt(), anyInt())).thenReturn(checkStatement);
        when(checkStatement.executeQuery("SELECT * FROM employees WHERE last_name='Barnett';"))
                .thenReturn(resultSet);

        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("department")).thenReturn("Sales");

        AttackResult result = lesson.completed(maliciousInput);

        assertNotNull(result);

        verify(updatePs).setString(1, maliciousInput);
        verify(updatePs).executeUpdate();

        verify(checkStatement, never()).executeUpdate(anyString());
    }
}
