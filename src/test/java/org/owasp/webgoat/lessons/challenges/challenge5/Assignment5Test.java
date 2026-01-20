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
import org.springframework.util.StringUtils;

public class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized query and succeeds for valid Larry credentials")
    void login_usesParameterizedQuery_andReturnsSuccessForValidLarry() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

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

        // Act
        AttackResult result = assignment5.login("Larry", "secret");

        // Assert
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, "Larry");
        verify(preparedStatement).setString(2, "secret");
        verify(preparedStatement).executeQuery();

        // ensure no concatenation-based prepareStatement was used
        verify(connection, never()).prepareStatement(
                contains("userid = 'Larry' and password = 'secret'"));

        assertEquals("success", result.getLessonStatus().name().toLowerCase());
    }

    @Test
    @DisplayName("login fails if username is not Larry, enforcing original user restriction")
    void login_failsForNonLarryUser() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Mallory", "secret");

        // Assert
        // When username is not Larry, the method should short-circuit before DB access
        verifyNoInteractions(dataSource);
        assertEquals("failed", result.getLessonStatus().name().toLowerCase());
    }
}
