// Delta unit test for SqlInjectionChallenge.java
// Assumed package based on resolved_file_path; adjust if actual package differs.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

class SqlInjectionChallengeTest {

    @Test
    void registerNewUserShouldUseParameterizedQueryForExistenceCheckAndCreateUser() throws SQLException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

        Connection connection = mock(Connection.class);
        PreparedStatement existsStmt = mock(PreparedStatement.class);
        PreparedStatement insertStmt = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
                .thenReturn(existsStmt);
        when(connection.prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)")).thenReturn(insertStmt);
        when(existsStmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // user does not exist

        String username = "john";
        String email = "john@example.com";
        String password = "secret";

        // Act
        AttackResult result = challenge.registerNewUser(username, email, password);

        // Assert
        verify(connection).prepareStatement("select userid from sql_challenge_users where userid = ?");
        verify(existsStmt).setString(1, username);
        verify(existsStmt).executeQuery();

        verify(connection).prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)");
        verify(insertStmt).setString(1, username);
        verify(insertStmt).setString(2, email);
        verify(insertStmt).setString(3, password);
        verify(insertStmt).execute();

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse(); // informational success, not completion
    }

    @Test
    void registerNewUserShouldNotCreateUserWhenUserAlreadyExists() throws SQLException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionChallenge challenge = new SqlInjectionChallenge(dataSource);

        Connection connection = mock(Connection.class);
        PreparedStatement existsStmt = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
                .thenReturn(existsStmt);
        when(existsStmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // user already exists

        String username = "john";
        String email = "john@example.com";
        String password = "secret";

        // Act
        AttackResult result = challenge.registerNewUser(username, email, password);

        // Assert
        verify(connection).prepareStatement("select userid from sql_challenge_users where userid = ?");
        verify(existsStmt).setString(1, username);
        verify(existsStmt).executeQuery();

        // No insert should be attempted when user exists
        verify(connection, never()).prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)");
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
