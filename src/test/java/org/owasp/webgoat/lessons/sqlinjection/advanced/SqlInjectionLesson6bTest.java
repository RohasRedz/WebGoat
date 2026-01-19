package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.io.IOException;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta unit tests for SqlInjectionLesson6b focused on the changed behavior:
 * - Exceptions in getPassword() are now logged via Slf4j instead of printStackTrace.
 * - Functional behavior of completed() and getPassword() must remain intact.
 *
 * Mapped from:
 * src/main/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6b.java
 * to:
 * src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
 */
@Slf4j
public class SqlInjectionLesson6bTest {

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

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(statement);
        when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
    }

    @Test
    @DisplayName("completed() returns success when userid_6b matches password from DB (normal path)")
    void completedReturnsSuccessWhenUserIdMatchesPassword() throws IOException, SQLException {
        // Arrange
        String dbPassword = "secretPwd";
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn(dbPassword);

        // Act
        AttackResult result = lesson6b.completed(dbPassword);

        // Assert
        assertEquals(AttackResult.Category.SUCCESS, result.getCategory(),
                "Expected SUCCESS when userid_6b equals the password retrieved from DB");
    }

    @Test
    @DisplayName("completed() returns failed when userid_6b does not match password from DB (normal path)")
    void completedReturnsFailedWhenUserIdDoesNotMatchPassword() throws IOException, SQLException {
        // Arrange
        String dbPassword = "secretPwd";
        when(resultSet.first()).thenReturn(true);
        when(resultSet.getString("password")).thenReturn(dbPassword);

        // Act
        AttackResult result = lesson6b.completed("wrongPwd");

        // Assert
        assertEquals(AttackResult.Category.FAILED, result.getCategory(),
                "Expected FAILED when userid_6b does not equal the password retrieved from DB");
    }

    @Test
    @DisplayName("getPassword() falls back to default 'dave' when SQLException occurs and behavior remains stable")
    void getPasswordReturnsDefaultOnSqlException() throws Exception {
        // Arrange
        when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException("DB error"));

        // Act
        String password = lesson6b.getPassword();

        // Assert
        assertEquals("dave", password,
                "On SQLException, getPassword() should return the default value 'dave' and not throw");
    }

    @Test
    @DisplayName("getPassword() falls back to default 'dave' when generic Exception occurs and behavior remains stable")
    void getPasswordReturnsDefaultOnGenericException() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection failure"));

        // Act
        String password = lesson6b.getPassword();

        // Assert
        assertEquals("dave", password,
                "On generic Exception, getPassword() should return the default value 'dave' and not throw");
    }
}
