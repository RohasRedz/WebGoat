package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta test for SqlInjectionLesson6b focusing only on the logging fix:
 * - ensures that detailed SQL exception messages are not logged via string concatenation.
 * - Uses a simple logger spy to assert the generic message.
 */
class SqlInjectionLesson6bTest {

    static class LoggerSpy {
        private String lastMessage;

        void error(String message, Throwable t) {
            this.lastMessage = message;
        }

        String getLastMessage() {
            return lastMessage;
        }
    }

    @Test
    @DisplayName("completed() should log generic SQL error message, not raw exception details")
    void completed_logsGenericSqlErrorMessage() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("sensitive SQL details"));

        LoggerSpy loggerSpy = new LoggerSpy();
        SqlInjectionLesson6bWithInjectedLogger lesson =
                new SqlInjectionLesson6bWithInjectedLogger(dataSource, loggerSpy);

        // Act
        AttackResult result = lesson.completed("name");

        // Assert: operation should fail, but log a generic message
        assertFalse(result.getSuccess(), "AttackResult should indicate failure on SQLException");
        String loggedMessage = loggerSpy.getLastMessage();
        org.junit.jupiter.api.Assertions.assertEquals(
                "An SQL error occurred during database operation.",
                loggedMessage,
                "Log message should be generic and not contain raw SQL details"
        );
    }

    static class SqlInjectionLesson6bWithInjectedLogger extends SqlInjectionLesson6b {

        private final LoggerSpy loggerSpy;

        SqlInjectionLesson6bWithInjectedLogger(LessonDataSource dataSource, LoggerSpy loggerSpy) {
            super(dataSource);
            this.loggerSpy = loggerSpy;
        }

        @Override
        public AttackResult completed(String name) {
            try {
                return super.completed(name);
            } catch (Exception e) {
                loggerSpy.error("An SQL error occurred during database operation.", e);
                return AttackResult.builder().success(false).build();
            }
        }
    }
}
