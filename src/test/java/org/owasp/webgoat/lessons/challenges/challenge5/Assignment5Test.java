/* Delta tests for Assignment5 focusing on SQL parameterization and behavior. */
package org.owasp.webgoat.lessons.challenges.challenge5;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class Assignment5Test {

    private LessonDataSource dataSource;
    private Flags flags;
    private Assignment5 assignment5;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        flags = mock(Flags.class);
        assignment5 = new Assignment5(dataSource, flags);

        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void login_shouldUseParameterizedQueryAndSucceedForValidCredentials() throws Exception {
        String username = "Larry";
        String password = "correct-password";
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        AttackResult result = assignment5.login(username, password);

        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertThat(usedSql.toLowerCase())
                .contains("where userid = ?")
                .contains("and password = ?")
                .doesNotContain(username.toLowerCase())
                .doesNotContain(password.toLowerCase());

        verify(statement).setString(1, username);
        verify(statement).setString(2, password);

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isTrue();
    }

    @Test
    void login_shouldFailForInvalidCredentialsWhileStillUsingParameterizedQuery() throws Exception {
        String username = "Larry";
        String password = "wrong-password";
        when(resultSet.next()).thenReturn(false);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        AttackResult result = assignment5.login(username, password);

        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertThat(usedSql.toLowerCase())
                .contains("where userid = ?")
                .contains("and password = ?");

        verify(statement).setString(1, username);
        verify(statement).setString(2, password);

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }

    @Test
    void login_shouldRejectNonLarryUserBeforeHittingDatabase() throws Exception {
        String username = "Mallory";
        String password = "anything";

        AttackResult result = assignment5.login(username, password);

        verifyNoInteractions(dataSource);
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
