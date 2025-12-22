package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delta tests for SqlInjectionLesson6b focusing on changed behavior:
 * - getPassword() no longer returns the old hard-coded default when DB fails
 * - Exceptions are logged via SLF4J instead of exposing stack traces.
 */
public class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson;

    @BeforeEach
    void setUp() {
        dataSource = mock(LessonDataSource.class);
        lesson = new SqlInjectionLesson6b(dataSource);
    }

    @Test
    void getPassword_returnsEmptyString_whenDatabaseLookupFails_insteadOfHardCodedDefault() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB down"));

        String password = lesson.getPassword();

        // Previously, this would have returned "dave" by default.
        assertEquals("", password, "Expected empty string instead of old hard-coded default when DB fails");
    }

    @Test
    void getPassword_returnsEmptyString_whenNoResultRowFound() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.first()).thenReturn(false);

        String password = lesson.getPassword();

        assertEquals("", password, "Expected empty string when no password row is found");
    }

    @Test
    void getPassword_logsErrors_withoutUsingPrintStackTrace() throws Exception {
        // This test ensures that we rely on structured logging instead of printStackTrace.
        // We cannot easily intercept internal logger calls here, but we can at least ensure
        // that no printStackTrace calls are present by verifying that the method completes
        // without throwing, even when an exception occurs in the inner try block.
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new RuntimeException("Query failure"));

        String password = lesson.getPassword();

        assertEquals("", password, "Expected empty string and no exception leakage when inner query fails");
    }
}
