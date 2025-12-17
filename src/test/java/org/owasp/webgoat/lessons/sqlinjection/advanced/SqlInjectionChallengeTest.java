// Assumed package based on source file path; adjust if needed.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import org.junit.jupiter.api.BeforeEach;
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
 * Delta tests for SqlInjectionChallenge focused on:
 * - Using a parameterized PreparedStatement for the user-exists check.
 * - Preserving behavior for existing vs new user registration.
 */
class SqlInjectionChallengeTest {

    private LessonDataSource dataSource;
    private Connection connection;
    private PreparedStatement checkUserStatement;
    private PreparedStatement insertUserStatement;
    private ResultSet resultSet;

    private SqlInjectionChallenge challenge;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LessonDataSource.class);
        connection = mock(Connection.class);
        checkUserStatement = mock(PreparedStatement.class);
        insertUserStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        // First prepareStatement call is for the checkUserQuery, second for the INSERT
        when(connection.prepareStatement(startsWith("select userid from sql_challenge_users"))).thenReturn(checkUserStatement);
        when(connection.prepareStatement(startsWith("INSERT INTO sql_challenge_users"))).thenReturn(insertUserStatement);
        when(checkUserStatement.executeQuery()).thenReturn(resultSet);

        challenge = new SqlInjectionChallenge(dataSource);
    }

    @Test
    void registerNewUser_shouldUseParameterizedQueryForUserExistenceCheck() throws Exception {
        // Arrange
        String username = "alice";
        String email = "alice@example.com";
        String password = "Secret#123";
        when(resultSet.next()).thenReturn(false); // user does not exist

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        challenge.registerNewUser(username, email, password);

        // Assert
        // Verify the checkUserQuery was parameterized and did not inline username
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertThat(usedSql.toLowerCase())
                .contains("where userid = ?")
                .doesNotContain(username.toLowerCase());

        // Ensure the username is bound as a parameter, not concatenated
        verify(checkUserStatement).setString(1, username);
        verify(checkUserStatement).executeQuery();
    }

    @Test
    void registerNewUser_shouldReturnUserExistsWhenUserAlreadyPresent() throws Exception {
        // Arrange
        String username = "bob";
        String email = "bob@example.com";
        String password = "Password1!";
        when(resultSet.next()).thenReturn(true); // user already exists

        // Act
        AttackResult result = challenge.registerNewUser(username, email, password);

        // Assert
        // Should not attempt insert when user exists
        verify(insertUserStatement, never()).execute();
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }

    @Test
    void registerNewUser_shouldInsertNewUserWhenNotExisting() throws Exception {
        // Arrange
        String username = "carol";
        String email = "carol@example.com";
        String password = "Password1!";
        when(resultSet.next()).thenReturn(false); // user does not exist

        // Act
        AttackResult result = challenge.registerNewUser(username, email, password);

        // Assert
        verify(insertUserStatement).setString(1, username);
        verify(insertUserStatement).setString(2, email);
        verify(insertUserStatement).setString(3, password);
        verify(insertUserStatement).execute();

        assertThat(result).isNotNull();
        // For a successfully created user the attack result should be an informational (non-fail) result.
        assertThat(result.getLessonCompleted()).isFalse(); // WebGoat 'informationMessage' does not complete the lesson
    }
}
