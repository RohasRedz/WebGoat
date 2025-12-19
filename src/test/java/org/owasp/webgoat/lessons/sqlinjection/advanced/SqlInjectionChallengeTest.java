package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

public class SqlInjectionChallengeTest {

    @Test
    void registerNewUser_shouldUsePreparedStatementForUserExistenceCheck() throws Exception {
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        SqlInjectionChallenge endpoint = new SqlInjectionChallenge(dataSource);

        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement checkStmt = Mockito.mock(PreparedStatement.class);
        PreparedStatement insertStmt = Mockito.mock(PreparedStatement.class);
        ResultSet rs = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
            .thenReturn(checkStmt);
        when(checkStmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        when(connection.prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)")).thenReturn(insertStmt);

        AttackResult result =
            endpoint.registerNewUser("user1", "user1@example.com", "password");

        Mockito.verify(connection)
            .prepareStatement("select userid from sql_challenge_users where userid = ?");
        Mockito.verify(checkStmt).setString(1, "user1");
        assertThat(result).isNotNull();
    }

    @Test
    void registerNewUser_sqlInjectionInUsernameMustNotBypassCheck() throws Exception {
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        SqlInjectionChallenge endpoint = new SqlInjectionChallenge(dataSource);

        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement checkStmt = Mockito.mock(PreparedStatement.class);
        ResultSet rs = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
            .thenReturn(checkStmt);
        when(checkStmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        String injection = "user1' OR '1'='1";

        AttackResult result =
            endpoint.registerNewUser(injection, "e@example.com", "p");

        Mockito.verify(checkStmt).setString(1, injection);
        assertThat(result).isNotNull();
    }
}
