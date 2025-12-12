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
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta unit tests for {@link Assignment5} focusing only on the security fix:
 *
 * - SQL must be executed via parameterized PreparedStatement with placeholders.
 * - User-supplied values must be bound via setString, and no string concatenation into the SQL.
 */
public class Assignment5DeltaTest {

    @Test
    @DisplayName("login should use parameterized PreparedStatement with bound parameters")
    void loginUsesParameterizedQueryAndBindsUserInput() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-PLACEHOLDER");

        String username = "Larry";
        String password = "P@ssw0rd!";

        AttackResult result = assignment.login(username, password);

        assertThat(result.getStatus()).isEqualTo(SUCCESS);

        String usedSql = sqlCaptor.getValue();
        assertThat(usedSql)
                .contains("select password from challenge_users where userid = ? and password = ?");
        assertThat(usedSql)
                .doesNotContain(username)
                .doesNotContain(password);

        org.mockito.Mockito.verify(preparedStatement).setString(1, username);
        org.mockito.Mockito.verify(preparedStatement).setString(2, password);
    }

    @Test
    @DisplayName("login should not inline SQLi payload into the query string")
    void loginShouldNotInlineSqlInjectionPayload() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        String username = "Larry";
        String password = "' OR '1'='1";

        AttackResult result = assignment.login(username, password);

        assertThat(result.getStatus()).isEqualTo(FAIL);

        String usedSql = sqlCaptor.getValue();
        assertThat(usedSql)
                .contains("where userid = ? and password = ?");
        assertThat(usedSql)
                .doesNotContain(password);

        org.mockito.Mockito.verify(preparedStatement).setString(1, username);
        org.mockito.Mockito.verify(preparedStatement).setString(2, password);
    }

    @Test
    @DisplayName("login should still reject empty credentials as before")
    void loginStillRejectsEmptyParameters() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment = new Assignment5(dataSource, flags);

        AttackResult result1 = assignment.login("", "password");
        AttackResult result2 = assignment.login("Larry", "");

        assertThat(StringUtils.hasText("")).isFalse();
        assertThat(result1.getStatus()).isEqualTo(FAIL);
        assertThat(result2.getStatus()).isEqualTo(FAIL);
    }
}
