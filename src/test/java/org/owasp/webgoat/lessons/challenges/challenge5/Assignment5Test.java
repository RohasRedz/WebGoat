package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
import org.owasp.webgoat.lessons.challenges.Flags;

@ExtendWith(MockitoExtension.class)
class Assignment5Test {

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
    @DisplayName("login() should bind user input as parameters and not build SQL via string concatenation")
    void login_usesParameterizedQueryAndSucceedsForValidUser() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "secret";
        String expectedSql =
                "select password from challenge_users where userid = ? and password = ?";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(expectedSql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Act
        var result = assignment5.login(username, password);

        // Assert: verify prepared statement SQL and bound parameters
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        // Note: we can't capture the SQL from prepareStatement directly because it's already matched
        // by value in when(...). Instead, assert on setString bindings which prove parameterization.
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        // first parameter = username, second parameter = password
        org.mockito.Mockito.verify(preparedStatement, org.mockito.Mockito.times(2))
                .setString(indexCaptor.capture(), valueCaptor.capture());

        assertTrue(indexCaptor.getAllValues().contains(1));
        assertTrue(indexCaptor.getAllValues().contains(2));
        assertTrue(valueCaptor.getAllValues().contains(username));
        assertTrue(valueCaptor.getAllValues().contains(password));

        // Also verify that no concatenated SQL is used by checking the SQL string constant
        // in the prepared statement creation
        org.mockito.Mockito.verify(connection)
                .prepareStatement(expectedSql);

        assertTrue(result.isLessonCompleted());
        assertEquals("FLAG-5", result.getFeedbackArgs()[0]);
    }

    @Test
    @DisplayName("login() should fail when credentials do not match, still using parameterized query")
    void login_failsForInvalidPasswordWithParameterizedQuery() throws Exception {
        // Arrange
        String username = "Larry";
        String password = "wrong";
        String expectedSql =
                "select password from challenge_users where userid = ? and password = ?";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(expectedSql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        var result = assignment5.login(username, password);

        // Assert: query still uses parameterized SQL, and result indicates failure
        org.mockito.Mockito.verify(connection)
                .prepareStatement(expectedSql);
        org.mockito.Mockito.verify(preparedStatement).setString(1, username);
        org.mockito.Mockito.verify(preparedStatement).setString(2, password);

        assertTrue(result.isAssignmentFailed());
    }
}
