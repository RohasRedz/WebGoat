package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for security fix in Assignment5:
 * - Verify that login with correct credentials yields a successful AttackResult.
 * - Verify that PreparedStatement uses parameter placeholders (no string concatenation).
 */
class Assignment5Test {

    @Test
    void loginWithValidCredentialsShouldSucceed() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry";
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isTrue();

        // Delta-specific assertion: verify parameterized SQL is used
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        assertThat(sql)
            .contains("userid = ?")
            .contains("password = ?")
            .doesNotContain(username)
            .doesNotContain(password);

        verify(preparedStatement).setString(eq(1), eq(username));
        verify(preparedStatement).setString(eq(2), eq(password));
    }

    @Test
    void loginWithInvalidUserShouldFailBeforeDatabaseInteraction() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "NotLarry";
        String password = "secret";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
        // Delta behavior: Since the SQL is parameterized, no SQL is constructed, but here
        // we also ensure that for invalid user, we short-circuit before any DB call.
        Mockito.verifyNoInteractions(dataSource);
    }
}
