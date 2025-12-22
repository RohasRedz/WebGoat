// Assuming the production class is in this package based on the resolved_file_path.
// TODO: Adjust package if the actual package differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.*;
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

/**
 * Delta tests for Assignment5 focusing ONLY on the changed SQL construction
 * related to the SQL injection vulnerability.
 *
 * The fix replaced string-concatenated SQL with a parameterized PreparedStatement.
 * These tests assert:
 *  - The query string contains parameter placeholders instead of concatenated user input.
 *  - The user-supplied username and password are bound via setString on the prepared statement.
 */
class Assignment5Test {

    @Test
    @DisplayName("login() should use parameterized query and bind username and password via setString")
    void login_usesParameterizedPreparedStatement_andBindsUserInput() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Flags flags = mock(Flags.class);

        when(dataSource.getConnection()).thenReturn(connection);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("flag-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "somePassword' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        // 1) Ensure success path still works (behavior not broken)
        assertNotNull(result);
        assertTrue(result.isLessonCompleted(), "Expected successful AttackResult for valid credentials");

        // 2) Assert that the SQL string uses parameter placeholders instead of inlined user input
        String usedSql = sqlCaptor.getValue();
        assertNotNull(usedSql, "Prepared SQL should not be null");
        assertTrue(
                usedSql.toLowerCase().contains("where userid = ?"),
                "SQL must use parameter placeholder for userid");
        assertTrue(
                usedSql.toLowerCase().contains("and password = ?"),
                "SQL must use parameter placeholder for password");

        assertFalse(
                usedSql.contains(username) || usedSql.contains(password),
                "User-supplied values must NOT be concatenated into the SQL string");

        // 3) Verify that user input is passed only via bind parameters
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // Ensure that executeQuery is still invoked
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("login() should reject requests with missing username or password before hitting the database")
    void login_rejectsMissingInput_withoutTouchingDatabase() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult noUser = assignment5.login("", "password");
        AttackResult noPassword = assignment5.login("Larry", "");

        // Assert
        assertFalse(noUser.isLessonCompleted(), "Missing username should not complete lesson");
        assertFalse(noPassword.isLessonCompleted(), "Missing password should not complete lesson");

        // Critical for security: avoid even building or executing SQL if inputs are missing.
        verifyNoInteractions(dataSource);
    }
}
