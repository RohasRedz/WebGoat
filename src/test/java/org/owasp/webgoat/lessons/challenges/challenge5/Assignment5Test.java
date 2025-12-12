package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing on changed behavior:
 * - SQL is now parameterized instead of concatenating user input.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized SQL query with bound parameters")
    void loginShouldUseParameterizedSql() throws Exception {
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Flags flags = Mockito.mock(Flags.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenReturn(true);
        Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "secretPassword";

        AttackResult result = assignment5.login(username, password);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                usedSql,
                "SQL must use placeholders instead of concatenated user input");

        Mockito.verify(preparedStatement).setString(eq(1), eq(username));
        Mockito.verify(preparedStatement).setString(eq(2), eq(password));

        assertTrue(result.getLessonCompleted(), "Login with correct credentials should still succeed");
    }
}
