package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests focusing on the security fix in Assignment5:
 * - Verifies that the login method uses parameterized SQL instead of string concatenation.
 */
class Assignment5Test {

    @Test
    @DisplayName("login() should bind username and password as PreparedStatement parameters")
    void loginUsesParameterizedQueryWithBoundParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "somePassword' OR '1'='1";

        // Act
        assignment5.login(username, password);

        // Assert
        // 1) SQL text must use parameter markers, not string concatenation
        String sqlUsed = sqlCaptor.getValue();
        assertEquals(
                "select password from challenge_users where userid = ? and password = ?",
                sqlUsed,
                "SQL must use parameter placeholders to prevent SQL injection");

        // 2) Verify that user-controlled parameters are bound via setString()
        verify(preparedStatement).setString(eq(1), eq(username));
        verify(preparedStatement).setString(eq(2), eq(password));

        // 3) Ensure the query is executed
        verify(preparedStatement).executeQuery();

        // 4) Ensure no alternate, concatenated SQL was created (implicit by absence of string building here)
        verifyNoMoreInteractions(preparedStatement);
    }
}
