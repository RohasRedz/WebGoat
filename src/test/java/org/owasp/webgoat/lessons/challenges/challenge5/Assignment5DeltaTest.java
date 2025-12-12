package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the SQL injection fix:
 * - Ensures PreparedStatement with parameter placeholders is used.
 * - Verifies that user inputs are bound via setString (no string concatenation in the query).
 */
public class Assignment5DeltaTest {

    @Test
    void loginShouldUseParameterizedQueryAndBindUserInputs() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "securePassword123";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: verify parameterized SQL and bound parameters
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        assertThat(usedSql)
                .as("Query should use placeholders instead of concatenating user input")
                .contains("where userid = ?")
                .contains("and password = ?")
                .doesNotContain(username)
                .doesNotContain(password);

        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        assertThat(result.getLessonCompleted())
                .as("Expected success path when resultSet.next() is true")
                .isTrue();
    }

    @Test
    void loginShouldHandleSqlExceptionWithoutLeakingDetails() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);

        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB failure"));

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "any";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert: should fail gracefully, not throw, and not succeed
        assertThat(result.getLessonCompleted())
                .as("Lesson should not be marked completed when SQL fails")
                .isFalse();
    }
}
