package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focused on the fixed SQL injection vulnerability.
 *
 * Path mapping rule:
 *   Source : src/main/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java
 *   Test   : src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
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
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void login_usesParameterizedQuery_andBindsUserInputs() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry'; DROP TABLE challenge_users; --";
        String password = "pass' OR '1'='1";

        AttackResult result = assignment5.login(username, password);

        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);
        verify(preparedStatement, times(1)).setString(Mockito.eq(1), arg1.capture());
        verify(preparedStatement, times(1)).setString(Mockito.eq(2), arg2.capture());

        assertEquals(username, arg1.getValue(), "Username should be bound as first parameter");
        assertEquals(password, arg2.getValue(), "Password should be bound as second parameter");
        verify(preparedStatement, times(1)).executeQuery();

        assertTrue(result.getLessonCompleted(), "Successful query should still yield a success result");
    }

    @Test
    void login_returnsFailureWhenNoResult_evenWithInjectionPayload() throws Exception {
        when(resultSet.next()).thenReturn(false);

        String username = "Larry' OR '1'='1";
        String password = "anything' OR '1'='1";

        AttackResult result = assignment5.login(username, password);

        assertTrue(!result.getLessonCompleted(), "No rows should result in failure even for injection payloads");
        verify(preparedStatement, times(1)).executeQuery();
    }
}
