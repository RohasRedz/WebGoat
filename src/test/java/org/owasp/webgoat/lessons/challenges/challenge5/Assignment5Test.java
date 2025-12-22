// Assuming the same package as the class under test; adjust if the actual package differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests focusing on the SQL injection fix in Assignment5.login.
 *
 * The vulnerability fix changed the SQL construction from string concatenation
 * to a parameterized PreparedStatement with bind parameters.
 *
 * These tests verify that:
 *  - The SQL sent to the database uses parameter placeholders (no concatenated user input).
 *  - User-controlled values are only provided via setString(..) bindings.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login uses parameterized query and binds username and password as parameters")
    void login_usesParameterizedQueryAndBindsParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // ensure success path
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry'";
        String password = "anything' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1) Ensure the SQL sent to the driver uses placeholders and not concatenation
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        // The fixed code uses: "select password from challenge_users where userid = ? and password = ?"
        // We assert that the query contains '?' markers and no raw username/password
        org.junit.jupiter.api.Assertions.assertTrue(
                sql.contains("userid = ?") && sql.contains("password = ?"),
                "SQL should use parameter placeholders for userid and password");
        org.junit.jupiter.api.Assertions.assertFalse(
                sql.contains(username) || sql.contains(password),
                "SQL string must not directly contain user-controlled username or password");

        // 2) Ensure parameters are bound correctly in the expected order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // 3) Ensure a query is executed and success path is reached
        verify(preparedStatement).executeQuery();
        org.junit.jupiter.api.Assertions.assertTrue(result.getLessonCompleted(), "Login should succeed for valid user");

        // 4) Sanity check: the input itself is non-empty and contains characters that would have been dangerous
        org.junit.jupiter.api.Assertions.assertTrue(StringUtils.hasText(username));
        org.junit.jupiter.api.Assertions.assertTrue(StringUtils.hasText(password));
    }

    @Test
    @DisplayName("login with invalid user still uses prepared statement and does not concatenate input")
    void login_invalidUserStillUsesPreparedStatement() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "NotLarry' OR '1'='1";
        String password = "pw";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        org.junit.jupiter.api.Assertions.assertTrue(
                sql.contains("userid = ?") && sql.contains("password = ?"),
                "SQL should use parameter placeholders for userid and password");
        org.junit.jupiter.api.Assertions.assertFalse(
                sql.contains(username) || sql.contains(password),
                "SQL string must not directly contain user-controlled username or password");

        // Parameters should still be bound, even though the application logic will reject the user later.
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        // Business rule: non-Larry users are rejected
        org.junit.jupiter.api.Assertions.assertFalse(result.getLessonCompleted(), "Login for non-Larry must fail");
    }
}
