package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

public class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized query and does not concatenate user input into SQL")
    void login_usesParameterizedQuery_noSqlConcatenation() throws Exception {
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
        when(flags.getFlag(5)).thenReturn("flag-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Use clearly malicious input to ensure it is treated as data, not SQL
        String username = "Larry' OR '1'='1";
        String password = "pass' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1) Verify the prepared statement is created with placeholders (no concatenation)
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");

        // 2) Verify user-controlled input is bound as parameters, not concatenated
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // 3) Behavior remains successful when resultSet.next() is true
        // (indirectly validates that the execution path is unchanged aside from parameterization)
        assertEquals(true, result.getLessonCompleted());
    }

    @Test
    @DisplayName("login fails when username or password is empty (unchanged validation behavior)")
    void login_rejectsEmptyCredentials() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult resultEmptyUser = assignment5.login("", "password");
        AttackResult resultEmptyPassword = assignment5.login("Larry", "");

        // Assert
        // These assertions ensure that input validation behavior surrounding the fixed code path
        // remains intact after the parameterization change.
        assertEquals(false, resultEmptyUser.getLessonCompleted());
        assertEquals(false, resultEmptyPassword.getLessonCompleted());

        // No DB interaction should occur when validation fails
        verifyNoInteractions(dataSource);
        verifyNoInteractions(flags);
    }
}
