package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.owasp.webgoat.container.assignments.AttackResult.Status.SUCCESS;
import static org.owasp.webgoat.container.assignments.AttackResult.Status.FAIL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta unit tests for Assignment5 focusing on the SQL injection fix (VULN-004).
 */
class Assignment5SecurityTest {

    @Test
    @DisplayName("VULN-004: Valid credentials for Larry should still authenticate successfully")
    void loginWithValidCredentialsSucceeds() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry";
        String password = "correct-password";

        AttackResult result = assignment.login(username, password);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SUCCESS);
    }

    @Test
    @DisplayName("VULN-004: SQL injection payload in password must not bypass authentication")
    void sqlInjectionInPasswordDoesNotBypassAuthentication() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(false);

        String username = "Larry";
        String passwordInjection = "anything' OR '1'='1";

        AttackResult result = assignment.login(username, passwordInjection);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(FAIL);

        Mockito.verify(preparedStatement).setString(1, username);
        Mockito.verify(preparedStatement).setString(2, passwordInjection);
    }

    @Test
    @DisplayName("VULN-004: Non-Larry username is rejected before executing SQL")
    void nonLarryUserRejectedWithoutConsideringSqlPayload() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        String username = "Eve' OR '1'='1";
        String password = "irrelevant";

        AttackResult result = assignment.login(username, password);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(FAIL);
        Mockito.verifyNoInteractions(dataSource, flags);
    }
}
