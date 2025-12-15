package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for {@link Assignment5} focusing only on:
 * - Use of parameterized PreparedStatement instead of string concatenation with user input.
 */
class Assignment5Test {

    @Test
    void loginShouldUsePreparedStatementParametersForUserInputs() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "p@ssw0rd";
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Act
        assignment5.login(username, password);

        // Assert
        // Verify that the SQL string uses parameter placeholders instead of concatenated values
        String usedSql = sqlCaptor.getValue();
        assertThat(usedSql)
            .isEqualTo("select password from challenge_users where userid = ? and password = ?");

        // Verify user-supplied values are bound as parameters, not concatenated into the SQL
        verify(preparedStatement).setString(eq(1), eq(username));
        verify(preparedStatement).setString(eq(2), eq(password));
    }

    @Test
    void loginShouldNotSucceedForSqlInjectionPayloadsWhenUsingPreparedStatement() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String passwordInjection = "' OR '1'='1";
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select password from challenge_users where userid = ? and password = ?"))
            .thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // With a proper PreparedStatement, an injection payload should behave like a normal string;
        // we simulate that the DB does NOT return a row for this combination.
        when(resultSet.next()).thenReturn(false);

        // Act
        var result = assignment5.login(username, passwordInjection);

        // Assert
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
