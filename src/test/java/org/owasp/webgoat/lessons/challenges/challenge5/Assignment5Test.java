// File: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    }

    @Test
    void login_withCorrectCredentials_usesParameterizedQueryAndSucceeds() throws Exception {
        when(flags.getFlag(5)).thenReturn("FLAG-5");
        when(resultSet.next()).thenReturn(true);

        AttackResult result = assignment5.login("Larry", "password123");

        // Verify that parameterized query is used
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "password123");

        assertTrue(result.getLessonCompleted(), "Expected challenge to be solved for correct credentials");
    }

    @Test
    void login_withSqlInjectionAttempt_doesNotThrowAndFailsAuthentication() throws Exception {
        // Simulate no rows returned even when an injection string is used
        when(resultSet.next()).thenReturn(false);

        String injectionUsername = "Larry' OR '1'='1";
        String injectionPassword = "anything' OR '1'='1";

        AttackResult result = assignment5.login(injectionUsername, injectionPassword);

        // Ensure still using parameterized query rather than concatenated SQL
        verify(connection).prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, injectionUsername);
        verify(preparedStatement).setString(2, injectionPassword);

        assertFalse(result.getLessonCompleted(), "SQL injection attempt should not bypass authentication");
    }
}
