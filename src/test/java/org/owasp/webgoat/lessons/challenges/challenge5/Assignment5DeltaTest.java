// Delta_UnitTest_Agent
// NOTE: Package is inferred from the source file's package declaration.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta unit tests focused only on the behavior changed to fix the SQL injection vulnerability
 * in Assignment5#login.
 *
 * Key expectations after the fix:
 * - The SQL query is parameterized and no longer built via string concatenation of user input.
 * - Valid credentials still succeed, invalid credentials still fail.
 *
 * These tests mock JDBC interactions and verify that user input is bound via
 * PreparedStatement parameters rather than concatenated into the SQL string.
 */
public class Assignment5DeltaTest {

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
        when(connection.prepareStatement(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void login_usesParameterizedQueryAndBindsUserInputs() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "SecretPass123";
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: success path still works
        assertTrue(result.getLessonCompleted(), "Expected login to succeed for correct credentials");

        // Assert: SQL query is parameterized (contains '?' placeholders, not concatenated inputs)
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        // We care only about the presence of placeholders and absence of raw user values
        assertTrue(
            usedSql.toLowerCase().contains("userid = ?") && usedSql.toLowerCase().contains("password = ?"),
            "Expected parameterized query with '?' placeholders for userid and password"
        );
        // Ensure raw user values were not concatenated into the SQL string
        assertTrue(
            !usedSql.contains(username) && !usedSql.contains(password),
            "User-supplied username and password must not be concatenated into the SQL string directly"
        );

        // Assert: user input is passed via PreparedStatement parameters in the correct order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    void login_failsGracefullyWithWrongPasswordWhileStillUsingParameters() throws Exception {
        // Arrange
        String username = "Larry";
        String wrongPassword = "Bad' OR '1'='1"; // classic injection payload, must not alter logic
        when(resultSet.next()).thenReturn(false); // no row returned ⇒ login fails

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        AttackResult result = assignment5.login(username, wrongPassword);

        // Assert: login should fail (no SQL injection possible)
        assertTrue(
            !result.getLessonCompleted(),
            "Expected login to fail when password is incorrect, even if it contains SQL injection payload"
        );

        // Assert: verify still uses parameterized query without concatenating payload
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertTrue(
            usedSql.toLowerCase().contains("userid = ?") && usedSql.toLowerCase().contains("password = ?"),
            "Expected parameterized query to be used for login check"
        );
        assertTrue(
            !usedSql.contains(username) && !usedSql.contains(wrongPassword),
            "Injection payload must not appear in the SQL string itself"
        );

        // Assert: payload is bound as a parameter, not altering the query structure
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, wrongPassword);
    }

    @Test
    void login_returnsChallengeSolvedFeedbackOnSuccess() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "SecretPass123";
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: verify that the success feedback still contains the expected flag argument
        assertTrue(result.getLessonCompleted(), "Expected challenge to be marked as solved");
        assertEquals(
            "FLAG-5",
            result.getFeedbackArgs()[0],
            "Expected the same flag to be returned after refactoring the SQL query"
        );
    }
}
