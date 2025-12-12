package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta unit tests for {@link Assignment5}:
 * Verifies the SQL injection fix by ensuring:
 * 1) The SQL query uses parameter placeholders (?) instead of direct concatenation.
 * 2) The user-supplied parameters are bound via PreparedStatement#setString.
 */
public class Assignment5Test {

    @Test
    void login_shouldUseParameterizedQueryAndBindUserInputs() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "somePassword";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert - SQL string must use placeholders and not contain the raw username/password
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();

        assertThat(usedSql.toLowerCase())
                .contains("where userid = ?")
                .contains("and password = ?");

        assertThat(usedSql).doesNotContain(username);
        assertThat(usedSql).doesNotContain(password);

        // Assert - user-supplied values are bound as parameters on PreparedStatement
        verify(preparedStatement).setString(eq(1), eq(username));
        verify(preparedStatement).setString(eq(2), eq(password));

        // Also assert that the original positive-flow behavior remains intact
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isTrue();
    }
}
