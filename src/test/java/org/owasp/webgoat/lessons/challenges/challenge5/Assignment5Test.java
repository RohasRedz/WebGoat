// Delta_UnitTest_Agent
// Package inferred from source file location; adjust if project structure differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing ONLY on the changed behavior:
 *  - SQL query must be parameterized and must not concatenate user input.
 */
class Assignment5Test {

    @Test
    void login_shouldUseParameterizedQueryWithoutSqlConcatenation() throws Exception {
        // Arrange
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

        String username = "Larry";
        String password = "P@ssw0rd";

        // Act
        assignment5.login(username, password);

        // Assert
        // 1. Validate that the prepared statement SQL uses placeholders rather than concatenation.
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertThat(usedSql)
                .as("SQL must use parameter placeholders instead of direct concatenation")
                .contains("userid = ?")
                .contains("password = ?")
                .doesNotContain(username)
                .doesNotContain(password);

        // 2. Validate that user-supplied values are bound through setString on the statement.
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
    }
}
