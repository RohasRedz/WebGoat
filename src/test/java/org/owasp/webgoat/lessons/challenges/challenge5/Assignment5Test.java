// File: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment5;

    @BeforeEach
    void setUp() {
        dataSource = mock(LessonDataSource.class);
        flags = mock(Flags.class);
        assignment5 = new Assignment5(dataSource, flags);
    }

    @Test
    void login_usesParameterizedQuery_andSucceedsForValidCredentials() throws Exception {
        // Arrange: valid user and password
        String username = "Larry";
        String password = "secret";
        String expectedSql =
                "select password from challenge_users where userid = ? and password = ?";

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(eq(expectedSql))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: query executed through prepared statement with parameters
        // If the old concatenated SQL were used, this test would fail because we only stub the
        // parameterized SQL string.
        org.junit.jupiter.api.Assertions.assertTrue(result.equals(success(assignment5)
                .feedback("challenge.solved")
                .feedbackArgs("FLAG-5")
                .build()));
    }

    @Test
    void login_doesNotAllowSqlInjectionViaUsername() throws Exception {
        // Arrange: attempt SQL injection through username
        String maliciousUsername = "Larry' OR '1'='1";
        String password = "anything";
        String expectedSql =
                "select password from challenge_users where userid = ? and password = ?";

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(eq(expectedSql))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // No rows returned because parameters are bound, not concatenated
        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment5.login(maliciousUsername, password);

        // Assert: login fails, demonstrating that injected SQL does not change query semantics
        org.junit.jupiter.api.Assertions.assertTrue(result.equals(failed(assignment5)
                .feedback("challenge.close")
                .build()));
    }
}
