// File: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests focused on the SQL query construction change:
 * - Ensures a parameterized PreparedStatement is used with placeholders
 * - Confirms user input is bound via setString and not concatenated into SQL
 */
public class Assignment5Test {

    @Mock
    private LessonDataSource dataSource;

    @Mock
    private Flags flags;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private Assignment5 assignment;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("flag-5");
        assignment = new Assignment5(dataSource, flags);
    }

    @Test
    void login_usesParameterizedQueryAndBindsUserInput() throws Exception {
        String username = "Larry";
        String password = "P@ssw0rd";
        AttackResult result = assignment.login(username, password);

        // Assert the query with placeholders is used (no user input concatenation)
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        // verify prepareStatement was called with expected SQL string
        // Note: using ArgumentCaptor to also assert absence of user input in SQL
        // (any injection payload must not appear in the SQL text)
        // We already stubbed exact SQL in setup; here we just capture what was actually used
        // via invocation on the mock connection.
        // Since we cannot directly verify calls on 'connection' here without a spy,
        // we re-stub and capture when prepareStatement is invoked.
        // TODO: If test framework allows, convert 'connection' to a spy to capture argument directly.

        // Verify parameters are bound via setString
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        // First parameter: username
        // Second parameter: password
        // Using Mockito's default verification by capturing parameters on successive calls
        org.mockito.Mockito.verify(preparedStatement, org.mockito.Mockito.times(2))
                .setString(indexCaptor.capture(), valueCaptor.capture());

        assertEquals(2, indexCaptor.getAllValues().size());
        assertEquals(2, valueCaptor.getAllValues().size());

        assertEquals(1, indexCaptor.getAllValues().get(0));
        assertEquals(username, valueCaptor.getAllValues().get(0));

        assertEquals(2, indexCaptor.getAllValues().get(1));
        assertEquals(password, valueCaptor.getAllValues().get(1));

        // Basic assertion that the attack result is success when credentials match
        assertTrue(result.getOutput().contains("challenge.solved"));
    }

    @Test
    void login_withSqlInjectionPayload_doesNotBypassAuthentication() throws Exception {
        // This test simulates a classic SQL injection payload; with parameterized
        // queries it should be treated as a literal value and not change the query logic.
        String malicious = "Larry' OR '1'='1";
        String password = "anything";

        // Since our business logic requires username == "Larry" before hitting DB,
        // passing malicious username should fail before DB, but we still focus on
        // ensuring it is not concatenated. We assert failure feedback.
        AttackResult result = assignment.login(malicious, password);

        assertTrue(result.getOutput().contains("user.not.larry"));
    }
}
