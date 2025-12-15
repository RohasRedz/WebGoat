package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the SQL-injection fix:
 * - Ensures the login method uses a parameterized PreparedStatement
 *   instead of building SQL directly via string concatenation.
 */
class Assignment5DeltaTest {

    @Test
    void login_shouldUseParameterizedQueryWithUserSuppliedCredentials() throws Exception {
        // Arrange
        LessonDataSource dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
        Connection connection = org.mockito.Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        Flags flags = org.mockito.Mockito.mock(Flags.class);

        org.mockito.Mockito.when(dataSource.getConnection()).thenReturn(connection);
        org.mockito.Mockito.when(connection.prepareStatement(org.mockito.Mockito.anyString()))
                .thenReturn(preparedStatement);
        org.mockito.Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        org.mockito.Mockito.when(resultSet.next()).thenReturn(true);
        org.mockito.Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "p@ssw0rd";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1. Ensure we are using a parameterized query (i.e., SQL text contains '?' placeholders),
        //    which is the key changed behavior to fix the SQL injection vulnerability.
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertThat(usedSql).contains("where userid = ? and password = ?");

        // 2. Ensure parameters are bound via setString rather than concatenated.
        org.mockito.Mockito.verify(preparedStatement).setString(1, username);
        org.mockito.Mockito.verify(preparedStatement).setString(2, password);

        // 3. Behaviorally, a successful row should still yield a success AttackResult.
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isTrue();
    }

    @Test
    void login_shouldFailWhenUsernameIsNotLarry_evenWithParameterizedQuery() throws Exception {
        // This test ensures the original lesson logic is preserved while still using
        // a parameterized PreparedStatement.
        LessonDataSource dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
        Connection connection = org.mockito.Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
        Flags flags = org.mockito.Mockito.mock(Flags.class);

        org.mockito.Mockito.when(dataSource.getConnection()).thenReturn(connection);
        org.mockito.Mockito.when(connection.prepareStatement(org.mockito.Mockito.anyString()))
                .thenReturn(preparedStatement);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String otherUser = "Eve";
        String password = "p@ssw0rd";

        // Act
        AttackResult result = assignment5.login(otherUser, password);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();

        // Even in this failure case, the code path should still be using a parameterized query;
        // verify SQL text and that parameter binding is attempted with the provided user.
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(connection, org.mockito.Mockito.never())
                .prepareStatement(org.mockito.Mockito.contains("userid = '" + otherUser + "'"));
        // When username is not 'Larry', the code returns before hitting the DB, so no SQL interactions.
        org.mockito.Mockito.verifyNoInteractions(preparedStatement);
    }
}
