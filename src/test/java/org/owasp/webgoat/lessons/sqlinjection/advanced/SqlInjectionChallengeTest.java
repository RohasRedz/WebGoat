package org.owasp.webgoat.lessons.sqlinjection.advanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Delta tests for SqlInjectionChallenge.
 *
 * Focus: Verify that the user-existence check now uses parameterized queries
 * (PreparedStatement with bind parameters) and does not build SQL with string concatenation.
 *
 * We do this indirectly by:
 *  - Verifying the prepared SQL string contains a single placeholder '?'
 *    instead of inlined user input.
 *  - Verifying that both safe and malicious usernames are bound via setString(1, ...).
 */
class SqlInjectionChallengeTest {

    @Test
    @DisplayName("Should use PreparedStatement with parameterized query for user existence check")
    void userExistenceCheckUsesParameterizedQuery() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement checkUserStmt = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        // Capture the SQL used to create the PreparedStatement for the existence check
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(checkUserStmt);
        when(checkUserStmt.executeQuery()).thenReturn(resultSet);
        // Simulate that user does not yet exist so insertion branch is taken
        when(resultSet.next()).thenReturn(false);

        // Insert PreparedStatement for INSERT branch as well
        PreparedStatement insertStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)"))
                .thenReturn(insertStmt);

        SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

        String safeUsername = "alice";
        String email = "alice@example.com";
        String password = "Password1!";

        // Act
        AttackResult result = challenge.registerNewUser(safeUsername, email, password);

        // Assert – existence check SQL
        String executedSql = sqlCaptor.getValue();
        assertThat(executedSql)
                .as("Existence-check query must use a placeholder instead of concatenating the username")
                .isEqualTo("select userid from sql_challenge_users where userid = ?");

        // Assert – username bound via parameter, not concatenated into SQL
        verify(checkUserStmt).setString(1, safeUsername);
        verify(checkUserStmt).executeQuery();

        // Assert – overall result should indicate successful creation
        assertThat(result.getLessonCompleted())
                .as("When user does not exist, account should be created successfully")
                .isTrue();
    }

    @Test
    @DisplayName("Malicious username must be safely bound as parameter (no SQL injection)")
    void maliciousUsernameIsBoundSafely() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement checkUserStmt = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(checkUserStmt);
        when(checkUserStmt.executeQuery()).thenReturn(resultSet);
        // Simulate that user already exists to exercise that branch as well
        when(resultSet.next()).thenReturn(true);

        SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

        String maliciousUsername = "bob' OR '1'='1";
        String email = "bob@example.com";
        String password = "Password1!";

        // Act
        AttackResult result = challenge.registerNewUser(maliciousUsername, email, password);

        // Assert – verify the malicious username is passed as a bound parameter
        verify(checkUserStmt).setString(1, maliciousUsername);
        verify(checkUserStmt).executeQuery();

        // The presence of the user should be detected purely via the bound parameter,
        // not via injection. We only assert that the code path completed without
        // attempting any concatenated SQL; behavioral status is out of scope here.
        assertThat(result.getLessonCompleted())
                .as("Lesson completion is not the focus; test ensures parameter binding protects query structure")
                .isFalse();
    }
}
