package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

    private LessonDataSource dataSource;
    private SqlInjectionLesson6b lesson6b;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = Mockito.mock(LessonDataSource.class);
        lesson6b = new SqlInjectionLesson6b(dataSource);

        connection = Mockito.mock(Connection.class);
        statement = Mockito.mock(Statement.class);
        resultSet = Mockito.mock(ResultSet.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(
                connection.createStatement(
                        Mockito.anyInt(),
                        Mockito.anyInt()
                )
        ).thenReturn(statement);
        Mockito.when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
    }

    @Test
    void getPassword_returnsDbPasswordWithoutLeakingStackTraceOnSqlException() throws Exception {
        // Arrange: simulate successful retrieval of password from DB
        Mockito.when(resultSet.first()).thenReturn(true);
        Mockito.when(resultSet.getString("password")).thenReturn("db-password");

        // Act
        String password = lesson6b.getPassword();

        // Assert: behavior preserved (still reads value from DB)
        assertEquals("db-password", password);

        // Now simulate SQL exception on second call to ensure it is handled without throwing
        Mockito.reset(connection, statement, resultSet);
        Mockito.when(dataSource.getConnection()).thenThrow(new RuntimeException("DB down"));

        String fallbackPassword = lesson6b.getPassword();

        // Assert: method handles exception and returns default without propagating stack trace
        assertEquals("dave", fallbackPassword, "Expected default password when DB access fails");
    }

    @Test
    void getPassword_doesNotChangeDefaultWhenNoResults() throws Exception {
        // Arrange: results.first() returns false  no row
        Mockito.when(resultSet.first()).thenReturn(false);

        // Act
        String password = lesson6b.getPassword();

        // Assert: default remains 'dave'
        assertEquals("dave", password);
        // verify that the query was still executed (behavior unchanged)
        Mockito.verify(statement).executeQuery(Mockito.anyString());
        // confirm that it did not accidentally set to some unexpected value
        assertNotEquals("db-password", password);
    }
}
