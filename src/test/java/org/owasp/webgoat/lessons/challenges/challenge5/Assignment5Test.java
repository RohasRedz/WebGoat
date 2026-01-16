package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

@ExtendWith(MockitoExtension.class)
public class Assignment5Test {

    @Mock
    private LessonDataSource dataSource;

    @Mock
    private Flags flags;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private Assignment5 assignment5;

    @Test
    @DisplayName("login uses parameterized PreparedStatement and no longer concatenates user input into SQL")
    void login_usesPreparedStatementWithParameters() throws Exception {
        // Arrange
        String username = "Larry"; // required by implementation
        String password = "secret";
        String expectedSql =
            "select password from challenge_users where userid = ? and password = ?";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(expectedSql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("flag-5");

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertTrue(result.isLessonSolved());

        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

        // verify that parameterized SQL (with ?) is used and that user input is bound via setString
        verify(connection, times(1)).prepareStatement(expectedSql);
        verify(preparedStatement, times(2)).setString(stringCaptor.capture().equals("ignored") ? 0 : 1, stringCaptor.capture());
        // Because of Mockito + overloaded methods complexity above, assert individually:
        verify(preparedStatement, times(1)).setString(1, username);
        verify(preparedStatement, times(1)).setString(2, password);
    }

    @Test
    @DisplayName("login rejects non-Larry usernames before executing SQL")
    void login_rejectsNonLarryWithoutExecutingQuery() throws Exception {
        // Arrange
        String username = "Mallory";
        String password = "anything";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertTrue(result.isLessonFailed());
        // dataSource.getConnection() must never be called for non-Larry users
        verify(dataSource, times(0)).getConnection();
    }
}
