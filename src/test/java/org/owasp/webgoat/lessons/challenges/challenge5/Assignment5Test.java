package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

public class Assignment5Test {

    @Test
    @DisplayName("login uses prepared statement with parameters instead of string concatenation")
    void login_usesPreparedStatementWithBoundParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "safePassword123";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // Verify that the SQL uses parameter placeholders
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");

        // Verify that user input is bound via setString, not concatenated
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(preparedStatement, times(2)).setString(anyInt(), captor.capture());
        assertEquals(username, captor.getAllValues().get(0));
        assertEquals(password, captor.getAllValues().get(1));

        // Ensure the flow still succeeds when credentials match
        // success() path should be taken as in original behavior
        // (we don't assert internals of AttackResult, only that no exception occurs and statement executed)
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }
}
