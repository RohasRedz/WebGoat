package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on the fixed SQL injection behavior.
 *
 * Target file (after fix):
 * src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java
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

    @Test
    void login_usesParameterizedQueryAndBindsUserInputs() throws Exception {
        when(resultSet.next()).thenReturn(true);

        AttackResult result = assignment5.login("Larry", "secret");

        // Verify the SQL text uses placeholders instead of concatenating user input
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        // The fixed implementation must contain parameter placeholders
        // and must not directly embed username_login or password_login.
        // We only assert on the presence of '?' and the expected structure.
        org.junit.jupiter.api.Assertions.assertTrue(
                sql.contains("where userid = ? and password = ?"),
                "SQL must use parameter placeholders");

        // Verify that the user-supplied values are bound via setString, not concatenated
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "secret");

        // Ensure success behavior is preserved
        assertEquals(true, result.getLessonCompleted());
    }

    @Test
    void login_sqlInjectionAttemptDoesNotBypassAuthentication() throws Exception {
        // Simulate that query returns no rows, even if malicious input is provided
        when(resultSet.next()).thenReturn(false);

        String injectionPayload = "Larry' OR '1'='1";
        AttackResult result = assignment5.login("Larry", injectionPayload);

        // Ensure the payload is bound as a parameter, not executed as part of SQL logic
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, injectionPayload);

        // The injection should not lead to a successful login
        assertEquals(false, result.getLessonCompleted());
    }

    @Test
    void login_invalidUsernameStillFailsEvenWithCorrectPassword() throws Exception {
        // The method short-circuits on username != Larry; SQL should not be executed.
        AttackResult result = assignment5.login("Bob", "whatever");

        // Verify no DB interaction happens when username is not Larry
        verifyNoInteractions(connection);

        assertEquals(false, result.getLessonCompleted());
    }

    @Test
    void login_emptyInputsReturnRequiredFeedbackWithoutDbCall() throws Exception {
        AttackResult result = assignment5.login("", "");

        // Ensure we do not hit the database when inputs are empty
        verifyNoInteractions(connection);

        assertEquals(false, result.getLessonCompleted());
    }
}
