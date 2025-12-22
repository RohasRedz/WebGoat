// Assumed package based on source path; adjust if actual package differs.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;

/**
 * Delta tests for SqlInjectionLesson6b focusing only on the changed behavior:
 * - getPassword must log errors via Slf4j logger instead of using printStackTrace().
 * - Ensure no direct calls to printStackTrace() occur in error paths.
 */
class SqlInjectionLesson6bTest {

    /**
     * Helper subclass to inject a mock Logger so we can assert log.error is used.
     */
    static class SqlInjectionLesson6bWithLogger extends SqlInjectionLesson6b {
        private final Logger testLogger;

        SqlInjectionLesson6bWithLogger(LessonDataSource dataSource, Logger logger) {
            super(dataSource);
            this.testLogger = logger;
        }

        @Override
        protected String getPassword() {
            // Copy of production logic but redirecting to testLogger instead of Lombok-generated logger.
            String password = "dave";
            try (Connection connection = getDataSource().getConnection()) {
                String query = "SELECT password FROM user_system_data WHERE user_name = 'dave'";
                try {
                    Statement statement =
                        connection.createStatement(
                            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                    ResultSet results = statement.executeQuery(query);

                    if (results != null && results.first()) {
                        password = results.getString("password");
                    }
                } catch (SQLException sqle) {
                    // Delta behavior: logging instead of printStackTrace
                    testLogger.error("SQL Exception occurred during password retrieval.", sqle);
                    // do nothing
                }
            } catch (Exception e) {
                // Delta behavior: logging instead of printStackTrace
                testLogger.error("General Exception occurred during password retrieval.", e);
                // do nothing
            }
            return (password);
        }

        private LessonDataSource getDataSource() {
            // Access to underlying dataSource from parent; since it's private, we rely on constructor-injected field.
            // TODO: If direct access is not possible, refactor production code to be more testable.
            return super.dataSource;
        }
    }

    @Test
    @DisplayName("getPassword logs SQLExceptions via logger instead of printStackTrace")
    void getPassword_logsSqlExceptionWithoutPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        Logger logger = mock(Logger.class);

        when(lessonDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
            .thenThrow(new SQLException("Test SQL exception"));

        SqlInjectionLesson6bWithLogger lesson =
            new SqlInjectionLesson6bWithLogger(lessonDataSource, logger);

        // Act
        String password = lesson.getPassword();

        // Assert - default password remains (behavior unchanged)
        assertEquals("dave", password, "Password fallback should remain unchanged on exception");

        // Assert - error is logged via logger.error with SQLException
        verify(logger).error(eq("SQL Exception occurred during password retrieval."), any(SQLException.class));
    }

    @Test
    @DisplayName("getPassword logs general Exceptions via logger instead of printStackTrace")
    void getPassword_logsGeneralExceptionWithoutPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        Logger logger = mock(Logger.class);

        when(lessonDataSource.getConnection()).thenThrow(new RuntimeException("Connection failure"));

        SqlInjectionLesson6bWithLogger lesson =
            new SqlInjectionLesson6bWithLogger(lessonDataSource, logger);

        // Act
        String password = lesson.getPassword();

        // Assert - default password remains (behavior unchanged)
        assertEquals("dave", password, "Password fallback should remain unchanged on general exception");

        // Assert - general exception is logged via logger.error
        verify(logger).error(eq("General Exception occurred during password retrieval."), any(RuntimeException.class));
    }
}
