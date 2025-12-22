// File path: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * - Use of parameterized PreparedStatement instead of string concatenation for SQL.
 * - Ensuring that SQL injection payloads cannot bypass authentication.
 */
class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment5;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        flags = mock(Flags.class);
        assignment5 = new Assignment5(dataSource, flags);

        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(flags.getFlag(5)).thenReturn("FLAG-5");
    }

    @Nested
    class SuccessfulLoginTests {

        @Test
        @DisplayName("login should succeed for user 'Larry' with correct password and use parameterized query")
        void loginSucceedsForLarryWithCorrectPassword_usesParameterizedQuery() throws Exception {
            // Arrange
            String username = "Larry";
            String password = "correct-password";

            when(resultSet.next()).thenReturn(true);

            // Act
            AttackResult result = assignment5.login(username, password);

            // Assert
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(connection).prepareStatement(sqlCaptor.capture());

            String usedSql = sqlCaptor.getValue();
            // Ensure no direct concatenation of user inputs in SQL
            // The pattern must contain '?' placeholders instead of the raw username/password.
            org.junit.jupiter.api.Assertions.assertTrue(
                    usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
                    "SQL should use parameter placeholders for userid and password"
            );
            org.junit.jupiter.api.Assertions.assertFalse(
                    usedSql.contains(username) || usedSql.contains(password),
                    "SQL must not directly embed username or password literals"
            );

            // Ensure parameters are bound in the correct order
            verify(preparedStatement).setString(1, username);
            verify(preparedStatement).setString(2, password);

            // Ensure success result is returned
            org.junit.jupiter.api.Assertions.assertTrue(result.getLessonCompleted());
        }
    }

    @Nested
    class SqlInjectionDefenseTests {

        @Test
        @DisplayName("login should NOT succeed for SQL injection attempt in password")
        void loginFailsForSqlInjectionInPassword() throws Exception {
            // Arrange
            String username = "Larry";
            // Payload that would previously bypass concatenated SQL if not properly parameterized
            String password = "' OR '1'='1";

            // With proper parameterization, the query should not return any row
            when(resultSet.next()).thenReturn(false);

            // Act
            AttackResult result = assignment5.login(username, password);

            // Assert
            verify(preparedStatement).setString(1, username);
            verify(preparedStatement).setString(2, password);
            org.junit.jupiter.api.Assertions.assertFalse(result.getLessonCompleted(),
                    "SQL injection payload must not result in a successful login");
        }

        @Test
        @DisplayName("login should fail fast when username is not 'Larry' even with SQL injection payload")
        void loginFailsForNonLarryWithSqlInjectionPayload() throws Exception {
            // Arrange
            String username = "Admin' OR '1'='1";
            String password = "irrelevant";

            // Act
            AttackResult result = assignment5.login(username, password);

            // Assert
            // When username is not Larry, the DB must not be called at all
            verifyNoInteractions(dataSource);
            org.junit.jupiter.api.Assertions.assertFalse(result.getLessonCompleted());
        }
    }

    @Nested
    class InputValidationTests {

        @Test
        @DisplayName("login should return failure when username or password is blank")
        void loginFailsForBlankInputs() throws Exception {
            // Arrange & Act
            AttackResult result1 = assignment5.login("", "password");
            AttackResult result2 = assignment5.login("Larry", "");
            AttackResult result3 = assignment5.login("   ", "   ");

            // Assert
            org.junit.jupiter.api.Assertions.assertFalse(result1.getLessonCompleted());
            org.junit.jupiter.api.Assertions.assertFalse(result2.getLessonCompleted());
            org.junit.jupiter.api.Assertions.assertFalse(result3.getLessonCompleted());
        }
    }

    @Test
    @DisplayName("login should propagate checked exceptions from dataSource.getConnection")
    void loginPropagatesCheckedExceptions() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "pwd";
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("DB down"));

        // Act & Assert
        assertThrows(Exception.class, () -> assignment5.login(username, password));
    }
}
