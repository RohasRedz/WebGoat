package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(flags.getFlag(5)).thenReturn("FLAG-5");
    }

    @Test
    void login_withValidCredentials_returnsSuccess() throws Exception {
        when(resultSet.next()).thenReturn(true);

        AttackResult result = assignment5.login("Larry", "secret");

        assertEquals("success", result.getLessonStatus().name().toLowerCase());
    }

    @Test
    void login_withInvalidCredentials_returnsFailure() throws Exception {
        when(resultSet.next()).thenReturn(false);

        AttackResult result = assignment5.login("Larry", "wrong");

        assertEquals("failed", result.getLessonStatus().name().toLowerCase());
    }

    @Test
    void login_withSqlInjectionPayload_doesNotBypassAuthentication() throws Exception {
        when(resultSet.next()).thenReturn(false);

        AttackResult result = assignment5.login("Larry", "' OR '1'='1");

        assertEquals("failed", result.getLessonStatus().name().toLowerCase());
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "' OR '1'='1");
    }

    @Test
    void login_withMissingParameters_returnsRequiredFeedback() throws Exception {
        AttackResult result = assignment5.login("", "");

        assertEquals("failed", result.getLessonStatus().name().toLowerCase());
    }
}
