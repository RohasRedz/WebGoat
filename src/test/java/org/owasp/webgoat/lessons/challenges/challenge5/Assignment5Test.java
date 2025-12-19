package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.lessons.challenges.Flags;

public class Assignment5Test {

    @Test
    void login_withCorrectCredentialsShouldUseBoundParameters() throws Exception {
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 endpoint = new Assignment5(dataSource, flags);

        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement ps = Mockito.mock(PreparedStatement.class);
        ResultSet rs = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        var result = endpoint.login("Larry", "secret");

        Mockito.verify(connection)
            .prepareStatement("select password from challenge_users where userid = ? and password = ?");
        Mockito.verify(ps).setString(1, "Larry");
        Mockito.verify(ps).setString(2, "secret");
        assertThat(result.getLessonCompleted()).isTrue();
    }

    @Test
    void login_withSqlInjectionPayloadInPasswordShouldNotBypassAuthentication() throws Exception {
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 endpoint = new Assignment5(dataSource, flags);

        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement ps = Mockito.mock(PreparedStatement.class);
        ResultSet rs = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        String injection = "' OR '1'='1";

        var result = endpoint.login("Larry", injection);

        Mockito.verify(ps).setString(1, "Larry");
        Mockito.verify(ps).setString(2, injection);
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
