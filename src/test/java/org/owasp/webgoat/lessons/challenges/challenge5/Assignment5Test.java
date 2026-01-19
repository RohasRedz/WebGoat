package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on the fixed SQL injection behavior.
 *
 * This test file is derived from:
 * src/main/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java
 * → src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 */
public class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment5;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = Mockito.mock(LessonDataSource.class);
        flags = Mockito.mock(Flags.class);
        assignment5 = new Assignment5(dataSource, flags);

        connection = Mockito.mock(Connection.class);
        preparedStatement = Mockito.mock(PreparedStatement.class);
        resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    @DisplayName("login succeeds for correct Larry credentials using parameterized query")
    void loginSucceedsForValidLarryCredentials() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "password123";

        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertEquals(AttackResult.Category.SUCCESS, result.getCategory(),
                "Expected successful login for valid Larry credentials");
        assertEquals("FLAG-5", result.getFeedbackArgs()[0],
                "Expected flag value to be returned on successful login");
        Mockito.verify(preparedStatement).setString(1, username);
        Mockito.verify(preparedStatement).setString(2, password);
    }

    @Test
    @DisplayName("login fails for SQL injection-like password and does not bypass authentication")
    void loginFailsForSqlInjectionLikePassword() throws Exception {
        // Arrange
        String username = "Larry";
        String injectionPassword = "' OR '1'='1";

        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment5.login(username, injectionPassword);

        // Assert
        assertEquals(AttackResult.Category.FAILED, result.getCategory(),
                "Injection-like password must not bypass authentication");
        Mockito.verify(preparedStatement).setString(1, username);
        Mockito.verify(preparedStatement).setString(2, injectionPassword);
    }
}
