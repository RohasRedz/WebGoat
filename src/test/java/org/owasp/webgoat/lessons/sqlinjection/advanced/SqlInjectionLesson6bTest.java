// Delta_UnitTest_Agent
// NOTE: This test focuses specifically on the safer logging behavior introduced by the fix.
// Test path inferred from main path by replacing 'main' with 'test':
// src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java

package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.LoggerFactory;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword logs SQLExceptions using logger instead of printStackTrace")
    void getPassword_logsSQLException_viaLogger() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("Test SQL error"));

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Capture logs from the class under test
        Logger classLogger = (Logger) LoggerFactory.getLogger(SqlInjectionLesson6b.class);
        Level originalLevel = classLogger.getLevel();
        classLogger.setLevel(Level.ERROR);

        TestLogAppender appender = new TestLogAppender();
        appender.start();
        classLogger.addAppender(appender);

        try {
            // Act
            String password = lesson.getPassword();

            // Assert
            assertThat(password).isEqualTo("dave"); // default remains unchanged on error

            assertThat(appender.containsMessage("SQL Exception occurred while retrieving password"))
                    .isTrue();
            assertThat(appender.getEvents()).hasSize(1);
            assertThat(appender.getEvents().get(0).getLevel()).isEqualTo(Level.ERROR);
        } finally {
            // Cleanup logger modifications
            classLogger.detachAppender(appender);
            classLogger.setLevel(originalLevel);
        }
    }

    // Simple custom logback appender to capture log events for assertions.
    // This isolates our test to the logging change without altering production code.
    private static class TestLogAppender extends ch.qos.logback.core.AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent> {
        private final java.util.List<ch.qos.logback.classic.spi.ILoggingEvent> events = new java.util.ArrayList<>();

        @Override
        protected void append(ch.qos.logback.classic.spi.ILoggingEvent eventObject) {
            events.add(eventObject);
        }

        java.util.List<ch.qos.logback.classic.spi.ILoggingEvent> getEvents() {
            return events;
        }

        boolean containsMessage(String messagePart) {
            return events.stream().anyMatch(e -> e.getFormattedMessage().contains(messagePart));
        }
    }
}
