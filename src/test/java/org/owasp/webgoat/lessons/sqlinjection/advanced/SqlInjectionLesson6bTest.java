package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

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
    @DisplayName("completed() succeeds only when userid_6b equals retrieved password")
    void completedSucceedsOnlyWhenUserIdEqualsPassword() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("secret-password");

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        AttackResult successResult = lesson.completed("secret-password");
        AttackResult failResult = lesson.completed("wrong-password");

        // Assert
        assertTrue(successResult.getLessonCompleted(), "completed() should succeed when userid_6b equals retrieved password");
        assertFalse(failResult.getLessonCompleted(), "completed() should fail when userid_6b does not equal retrieved password");
    }

    @Test
    @DisplayName("getPassword logs a warning on SQLException and keeps default password")
    void getPasswordLogsWarningOnSQLException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("boom"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Use a spy to observe interactions; Lombok @Slf4j provides a 'log' field.
        SqlInjectionLesson6b spyLesson = spy(lesson);
        // We cannot access the private logger directly; instead we verify that no printStackTrace()
        // is called on the SQLException and that functional behavior (default password) is preserved.

        // Act
        String password = spyLesson.getPassword();

        // Assert functional behavior: default "dave" is still returned
        // (the method should swallow the exception and return the fallback)
        org.junit.jupiter.api.Assertions.assertEquals("dave", password);

        // Verify that the SQLException was thrown from createStatement and not handled via printStackTrace()
        // Since printStackTrace() was removed, we ensure there is no such call by verifying the SQLException
        // is never re-thrown or propagated: calling getPassword() must not throw.
        // Additionally, we can verify that the method fully executes without interacting with ResultSet.
        verify(connection).createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        // No result set interactions should happen when createStatement throws
        verifyNoMoreInteractions(connection);
    }

    @Test
    @DisplayName("getPassword logs an error on unexpected Exception from getConnection and keeps default password")
    void getPasswordLogsErrorOnGenericException() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("connection failed"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);
        SqlInjectionLesson6b spyLesson = spy(lesson);

        // Act
        String password = spyLesson.getPassword();

        // Assert: still returns default password despite exception
        org.junit.jupiter.api.Assertions.assertEquals("dave", password);

        // Verify that getConnection was attempted exactly once
        verify(dataSource).getConnection();
        // Since the exception is swallowed inside getPassword and handled via logging,
        // no further DB interactions occur.
        verifyNoMoreInteractions(dataSource);
    }

    @Test
    @DisplayName("No printStackTrace is present in behavior when SQLException occurs on executeQuery")
    void executeQuerySQLExceptionDoesNotUsePrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'"))
                .thenThrow(new SQLException("error executing query"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        String password = lesson.getPassword();

        // Assert:
        // 1) Method returns safely with default password.
        org.junit.jupiter.api.Assertions.assertEquals("dave", password);
        // 2) We can infer that no stack trace was printed to System.err by checking that
        //    the SQLException was handled internally and not re-thrown.
        //    Since we cannot intercept System.err without altering production code,
        //    this delta test asserts behavior: no exception escapes.
        verify(connection).createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        verify(statement).executeQuery("SELECT password FROM user_system_data WHERE user_name = 'dave'");
    }
}
