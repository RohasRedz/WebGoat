package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("completed() should still validate password using getPassword()")
    void completed_usesGetPasswordForValidation() throws IOException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = spy(new SqlInjectionLesson6b(dataSource));

        when(lesson.getPassword()).thenReturn("secretPwd");

        // Act
        AttackResult success = lesson.completed("secretPwd");
        AttackResult failure = lesson.completed("wrong");

        // Assert
        org.junit.jupiter.api.Assertions.assertTrue(success.getLessonCompleted());
        org.junit.jupiter.api.Assertions.assertFalse(failure.getLessonCompleted());
        verify(lesson, times(2)).getPassword();
    }

    @Test
    @DisplayName("getPassword() should not use printStackTrace and instead log via SLF4J")
    void getPassword_usesSlf4jLogging_insteadOfPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new java.sql.SQLException("DB error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        Logger logger = LoggerFactory.getLogger(SqlInjectionLesson6b.class);
        Logger spyLogger = spy(logger);

        // Replace static logger via reflection to assert logging behavior
        java.lang.reflect.Field logField = SqlInjectionLesson6b.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(null, spyLogger);

        // Act
        String password = lesson.getPassword();

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals("dave", password, "Fallback password should be returned");

        // Verify that an error is logged instead of using printStackTrace
        verify(spyLogger, atLeastOnce())
                .error(startsWith("SQL Exception in getPassword:"), any());

        // No System.err stack trace printing should occur — not directly verifiable here,
        // but ensured by code change (printStackTrace removed in updated implementation).
    }
}
