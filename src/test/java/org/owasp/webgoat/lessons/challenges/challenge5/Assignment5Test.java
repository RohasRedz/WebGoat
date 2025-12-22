package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * - The login method now uses a parameterized PreparedStatement instead of string concatenation.
 *
 * Covered behaviors:
 * 1) Successful login with correct credentials still works (regression guard).
 * 2) SQL injection attempts such as username = "Larry' OR '1'='1" do NOT bypass authentication
 *    (i.e., the prepared statement must be used and not string concatenation).
 */
class Assignment5Test {

    @Mock
    private LessonDataSource dataSource;

    @Mock
    private Flags flags;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private Assignment5 assignment5;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // For successful scenario, the specific behavior of resultSet.next()/getXxx()
        // will be configured per test so each test is isolated.
    }

    @Test
    @DisplayName("login should succeed for correct Larry credentials (behavior preserved after parameterization)")
    void login_withValidLarryCredentials_stillSucceeds() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "correct-password";

        // Simulate DB returning one matching row
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertNotNull(result);
        // WebGoat AttackResult typically exposes success state via isSuccess(), but
        // we avoid depending on its full API here and instead assert known behavior via toString().
        // If AttackResult has isSuccess(), prefer:
        // assertTrue(result.isSuccess());
        String asString = result.toString();
        // We at least ensure the flag value is present indicating the success path.
        // TODO: If AttackResult has an explicit API for checking success, use it instead.
        org.junit.jupiter.api.Assertions.assertTrue(asString.contains("FLAG-5"),
                "Expected success result to contain flag value for solved challenge");

        // Verify that a parameterized query was used and user inputs were bound.
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("login should NOT be bypassed by SQL injection style username when using prepared statement")
    void login_withSqlInjectionUsername_doesNotBypassAuthentication() throws Exception {
        // Arrange
        String injectionUsername = "Larry' OR '1'='1";
        String password = "anything";

        // Simulate DB returning no rows for the injected username,
        // which models the secure behavior when parameters are used.
        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment5.login(injectionUsername, password);

        // Assert
        assertNotNull(result);
        String asString = result.toString();
        // The failure feedback key in the original code is "challenge.close";
        // checking that the result is not the "challenge.solved" path is the essential property here.
        org.junit.jupiter.api.Assertions.assertFalse(asString.contains("challenge.solved"),
                "SQL injection input should not result in solved challenge");
        // TODO: If AttackResult exposes a boolean failure/success flag, assert explicitly.

        // Crucial security assertion: ensure the prepared statement is called with placeholders,
        // and the injection string is passed as a parameter, not concatenated into the SQL.
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, injectionUsername);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        // Also ensure that no alternate, concatenated query was used.
        // Since we stub only the parameterized query text, any attempt to use a different SQL
        // string would either not match this verify or cause an unstubbed call.
        verify(connection, times(1)).prepareStatement(anyString());
    }
}
