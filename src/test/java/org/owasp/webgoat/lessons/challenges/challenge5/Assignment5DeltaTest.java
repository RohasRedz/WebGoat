package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

public class Assignment5DeltaTest {

    @Test
    @DisplayName("login should use parameterized PreparedStatement and not embed raw user input in SQL")
    void login_usesParameterizedQuery_andBindsUserInput() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry' OR '1'='1";
        String password = "pass' OR '1'='1";

        AttackResult result = assignment5.login(username, password);

        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");

        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        verify(preparedStatement).executeQuery();

        assertThat(result.getLessonCompleted()).isTrue();
    }

    @Test
    @DisplayName("login should still enforce that username is 'Larry' even after SQL fix")
    void login_preservesBusinessRule_forLarryUser() throws Exception {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        AttackResult result = assignment5.login("NotLarry", "anyPassword");

        assertThat(result.getLessonCompleted()).isFalse();
    }
}
