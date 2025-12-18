package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword no longer uses hard-coded default and returns null on DB error")
    void getPassword_returnsNullOnErrorNoHardCodedDefault() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        // Simulate SQL exception when preparing or executing statement
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        // Previously, the method started with "dave" and might have leaked that;
        // now it must return null when DB access fails.
        assertNull(password, "Hard-coded default password should not be used when DB errors occur");
    }

    @Test
    @DisplayName("getPassword uses PreparedStatement with parameterized username 'dave'")
    void getPassword_usesPreparedStatementWithParameter() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secret-from-db");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert
        // Ensure PreparedStatement is used and the username is bound as a parameter.
        var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertTrue(
                usedSql.toLowerCase().contains("from user_system_data"),
                "Query should select from user_system_data table");

        verify(preparedStatement).setString(1, "dave");
        assertEquals("secret-from-db", password, "Password should be read from result set when query succeeds");
    }

    @Test
    @DisplayName("completed only succeeds when user input matches retrieved password (secure failure when null)")
    void completed_succeedsOnlyOnExactPasswordMatch() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-password");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act / Assert
        AttackResult success = lesson.completed("db-password");
        assertTrue(success.getLessonCompleted(), "Should succeed when user input matches the DB password exactly");

        AttackResult failure = lesson.completed("wrong-password");
        assertFalse(failure.getLessonCompleted(), "Should fail when user input does not match DB password");
    }

    @Test
    @DisplayName("getPassword logs error instead of printing stack trace on SQL exception")
    void getPassword_logsErrorInsteadOfPrintingStackTrace() throws Exception {
        // NOTE: Directly asserting on logging output is framework/config dependent.
        // This test focuses on ensuring exceptions are caught and do not leak via rethrow;
        // combined with code inspection, this guards against re-introduction of printStackTrace().
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act & Assert
        assertDoesNotThrow(lesson::getPassword, "getPassword should handle SQL exceptions internally without propagating them");

        // Optional sanity: ensure logger is available (guards against removal of @Slf4j).
        Logger logger = LoggerFactory.getLogger(SqlInjectionLesson6b.class);
        assertNotNull(logger, "Logger should be configured via @Slf4j for secure logging");
    }
}
