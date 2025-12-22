// Assumed package based on source path; adjust if actual package differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * - The login method must use a parameterized PreparedStatement (no SQL concatenation).
 * - Correctly binds username and password as parameters to the query.
 */
class Assignment5Test {

    @Test
    @DisplayName("login uses PreparedStatement with parameter placeholders and binds both parameters")
    void login_usesPreparedStatementAndBindsParameters() throws Exception {
        // Arrange
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        DataSource delegateDataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(lessonDataSource.getDataSource()).thenReturn(delegateDataSource);
        when(delegateDataSource.getConnection()).thenReturn(connection);

        // Capture the SQL passed to prepareStatement to ensure it uses ? placeholders
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);

        // Simulate successful authentication
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

        String username = "Larry";
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert - SQL is parameterized, not concatenated
        String usedSql = sqlCaptor.getValue();
        assertTrue(
            usedSql.contains("userid = ?") && usedSql.contains("password = ?"),
            "SQL must use parameter placeholders instead of string concatenation"
        );
        // Ensure no direct user-controlled content is concatenated into the SQL string
        assertTrue(
            !usedSql.contains(username) && !usedSql.contains(password),
            "SQL must not contain raw username or password values"
        );

        // Assert - parameters are bound in correct order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Assert - behavior remains successful flow
        assertTrue(result.getLessonCompleted(), "Login should still succeed for valid credentials");
        assertEquals("FLAG-5", result.getOutput(), "Flag should be returned on success");
    }

    @Test
    @DisplayName("login with incorrect password still fails after parameterization")
    void login_incorrectPasswordStillFailsWithParameterizedQuery() throws Exception {
        // Arrange
        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        DataSource delegateDataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(lessonDataSource.getDataSource()).thenReturn(delegateDataSource);
        when(delegateDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        // Simulate authentication failure
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "wrong-password");

        // Assert - still fails, demonstrating behavior preserved but now secure
        assertTrue(!result.getLessonCompleted(), "Login must fail for incorrect password");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "wrong-password");
    }
}
