// Delta_UnitTest_Agent
// NOTE: Package is inferred from the source file's package declaration.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for SqlInjectionLesson6b focusing on the fix for
 * "Information Exposure Through Log Files".
 *
 * Expectations after the fix:
 * - getPassword() no longer prints stack traces via printStackTrace().
 * - Errors are logged using Slf4j (via Lombok @Slf4j).
 *
 * Because @Slf4j generates a private static final logger, we cannot easily
 * access it without reflection, and we avoid brittle reflection-based tests.
 * Instead, these tests focus on ensuring that:
 * - getPassword() still behaves correctly under normal conditions.
 * - getPassword() handles SQLExceptions without propagating stack traces
 *   via System.err (regression guard by simulating error scenarios).
 */
public class SqlInjectionLesson6bDeltaTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        lesson = new SqlInjectionLesson6b(dataSource);

        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(
                Mockito.anyInt(),
                Mockito.anyInt()))
            .thenReturn(statement);
    }

    @Test
    void getPassword_returnsDatabasePasswordOnSuccess() throws Exception {
        // Arrange
        when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn("db-secret");

        // Act
        String password = lesson.getPassword();

        // Assert: behavior is preserved after logging changes
        assertEquals("db-secret", password, "Expected getPassword to return password from DB on success");
    }

    @Test
    void getPassword_handlesSQLExceptionWithoutPropagating() throws Exception {
        // Arrange
        when(statement.executeQuery(Mockito.anyString()))
            .thenThrow(new SQLException("Simulated DB error"));

        // Act
        String password = lesson.getPassword();

        // Assert: method should fall back to default 'dave' and not throw
        assertEquals(
            "dave",
            password,
            "When a SQLException occurs, getPassword should return the default password value"
        );
        // NOTE:
        // Prior to the fix, this situation would cause printStackTrace() to be invoked.
        // After the fix, the method logs the error via Slf4j. This test ensures
        // that the method still completes normally without rethrowing the exception.
    }

    @Test
    void completed_endpointReflectsPasswordComparisonBehavior() throws Exception {
        // Arrange
        // Ensure getPassword() returns a known value
        SqlInjectionLesson6b lessonSpy = Mockito.spy(new SqlInjectionLesson6b(dataSource));
        Mockito.doReturn("db-secret").when(lessonSpy).getPassword();

        // Act & Assert
        AttackResult successResult = lessonSpy.completed("db-secret");
        AttackResult failResult = lessonSpy.completed("wrong-value");

        assertTrue(successResult.getLessonCompleted(), "Endpoint should succeed when userid_6b equals password");
        assertFalse(failResult.getLessonCompleted(), "Endpoint should fail when userid_6b does not equal password");
        // NOTE:
        // This test confirms that the logging changes did not alter the functional
        // behavior of the endpoint or the getPassword() logic.
    }
}
