package org.owasp.webgoat.lessons.challenges.challenge5;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.lessons.challenges.Flags;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Delta tests for Assignment5 focusing on the secure SQL usage introduced in the fix.
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

    @Test
    void login_usesParameterizedQueryWithUserInputs() throws Exception {
        String username = "Larry";
        String password = "secret";

        when(resultSet.next()).thenReturn(true);

        assignment5.login(username, password);

        // Verify that the SQL string uses ? placeholders, not concatenated inputs
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                sql,
                "SQL must use parameter placeholders instead of concatenated user input"
        );

        // Verify that user input is passed via setString parameters rather than concatenation
        verify(preparedStatement).setString(1, eq(username));
        verify(preparedStatement).setString(2, eq(password));
    }
}
